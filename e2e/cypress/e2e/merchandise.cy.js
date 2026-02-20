describe('Merchandise + Rewards (E2E)', () => {
    const dismissToasts = () => {
        cy.get('body', {log: false}).then($body => {
            const hasToasts =
                $body.find('#toast-container .ngx-toastr, #toast-container .toast').length > 0;
            if (!hasToasts) return;

            if ($body.find('#toast-container .toast-close-button').length > 0) {
                cy.get('#toast-container .toast-close-button', {log: false})
                    .click({multiple: true, force: true});
            } else {
                cy.get('#toast-container .ngx-toastr, #toast-container .toast', {log: false})
                    .click({multiple: true, force: true});
            }

            cy.get('#toast-container .ngx-toastr, #toast-container .toast', {timeout: 10000, log: false})
                .should('not.exist');
        });
    };

    const login = () => {
        cy.intercept('POST', '**/authentication').as('login');

        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').clear().type('user5@email.com');
        cy.get('input[formcontrolname="password"]').clear().type('password');
        cy.get('button[type="submit"]').click();

        cy.wait('@login', {timeout: 15000}).its('response.statusCode').should('eq', 200);
        dismissToasts();
    };

    const goToCart = () => {
        cy.visit('/#/cart');
        cy.location('hash', {timeout: 10000}).should('include', '/cart');
        cy.wait('@getCart', {timeout: 15000}).its('response.statusCode').should('eq', 200);
    };

    const removeCatsShirtIfPresent = () => {
        cy.get('body').then($body => {
            if ($body.find('.cart-item-card').length === 0) return;
            if ($body.find('.cart-item-card:contains("Cats Shirt")').length === 0) return;

            cy.on('window:confirm', () => true);

            cy.contains('.cart-item-card', 'Cats Shirt').within(() => {
                cy.contains('button', 'Entfernen').click({force: true});
            });

            cy.wait('@getCart', {timeout: 15000});
        });
    };

    it('buys 2x Cats Shirt and credits reward points', () => {
        cy.intercept('GET', '**/api/v1/users/me/reward-points').as('rewardPoints');

        cy.intercept({method: 'GET', url: /\/api\/v1\/merchandise(\?.*)?$/}).as('merchList');

        cy.intercept('GET', '**/api/v1/cart').as('getCart');
        cy.intercept('POST', '**/api/v1/cart/items').as('addCartItem');
        cy.intercept('PATCH', '**/api/v1/cart/items/*').as('updateCartItemQty');
        cy.intercept('POST', '**/api/v1/cart/checkout').as('checkoutCart');

        login();

        cy.visit('/#/profile');
        cy.contains(/Prämienpunkte/i, {timeout: 10000}).click({force: true});
        cy.wait('@rewardPoints', {timeout: 15000}).then(rp => {
            const pointsBefore = (rp.response && rp.response.body && rp.response.body.rewardPoints) || 0;

            cy.visit('/#/merchandise');
            cy.wait('@merchList', {timeout: 15000}).then(m => {
                const list = (m.response && m.response.body) ? m.response.body : [];
                const cats = list.find(x => x && x.name === 'Cats Shirt');
                const rpPerUnit = cats ? (cats.rewardPointsPerUnit || 0) : 0;

                goToCart();
                removeCatsShirtIfPresent();

                cy.visit('/#/merchandise');
                cy.wait('@merchList', {timeout: 15000});

                cy.contains('.merch-title', 'Cats Shirt', {timeout: 10000})
                    .closest('.merch-card')
                    .contains('button', 'Zum Warenkorb hinzufügen')
                    .click({force: true});

                cy.wait('@addCartItem', {timeout: 15000}).its('response.statusCode').should('eq', 200);
                dismissToasts();

                goToCart();
                cy.contains('.cart-item-card', 'Cats Shirt', {timeout: 10000}).within(() => {
                    cy.get('select.qty-select').should('be.visible').select('2');
                });

                cy.wait('@updateCartItemQty', {timeout: 15000}).then(({request, response}) => {
                    expect(response && response.statusCode).to.eq(200);
                    expect(request.body).to.have.property('quantity', 2);
                });

                cy.contains('button', 'Zur Kassa', {timeout: 10000}).click({force: true});
                cy.contains('h2', 'Zahlung & Bestellung', {timeout: 10000}).should('exist');

                cy.contains('label', 'Kartennummer', {timeout: 10000})
                    .parent().find('input.form-control')
                    .clear().type('4242424242424242');

                cy.contains('label', 'Ablaufdatum', {timeout: 10000})
                    .parent().find('input.form-control')
                    .clear().type('0128');

                cy.contains('label', 'CVC', {timeout: 10000})
                    .parent().find('input.form-control')
                    .clear().type('123');

                cy.contains('button', 'Zahlungspflichtig bestellen', {timeout: 10000})
                    .click({force: true});

                cy.get('.modal-backdrop-custom', {timeout: 10000})
                    .should('be.visible')
                    .within(() => {
                        cy.get('input[type="checkbox"]').uncheck({force: true});
                        cy.contains('button', 'Bestellung abschließen').click({force: true});
                    });

                cy.wait('@checkoutCart', {timeout: 15000}).its('response.statusCode').should('eq', 201);

                cy.visit('/#/profile');
                cy.contains(/Prämienpunkte/i, {timeout: 10000}).click({force: true});
                cy.wait('@rewardPoints', {timeout: 15000}).then(after => {
                    const pointsAfter = (after.response && after.response.body && after.response.body.rewardPoints) || 0;
                    expect(pointsAfter).to.eq(pointsBefore + (rpPerUnit * 2));
                });
            });
        });
    });

    it('redeems a reward via detail view (adds it to cart with redeemedWithPoints=true)', () => {
        cy.intercept('GET', '**/api/v1/merchandise/rewards**').as('getRewards');
        cy.intercept('GET', '**/api/v1/users/me/reward-points').as('rewardPoints');
        cy.intercept('GET', '**/api/v1/users/me/total-cents-spent').as('totalSpent');
        cy.intercept({method: 'GET', url: /\/api\/v1\/merchandise\/\d+(\?.*)?$/}).as('getMerchById');

        cy.intercept('POST', '**/api/v1/cart/items').as('addCartItem');

        login();

        cy.visit('/#/merchandise/rewards');
        cy.wait('@rewardPoints', {timeout: 20000});
        cy.wait('@totalSpent', {timeout: 20000});
        cy.wait('@getRewards', {timeout: 20000});

        cy.contains('h1', 'Prämien', {timeout: 10000}).should('be.visible');

        cy.get('.merch-card a.merch-link', {timeout: 20000})
            .first()
            .scrollIntoView()
            .click({force: true});

        cy.wait('@getMerchById', {timeout: 20000});
        cy.contains('button', /Prämie zum Warenkorb hinzufügen/i, {timeout: 20000})
            .should('be.visible')
            .and('not.be.disabled')
            .scrollIntoView()
            .click({force: true});

        cy.wait('@addCartItem', {timeout: 20000}).then(({request, response}) => {
            expect(response && response.statusCode).to.eq(200);
            expect(request.body).to.have.property('redeemedWithPoints', true);
        });
    });

    it('shows current reward points in profile and navigates to rewards', () => {
        cy.intercept('GET', '**/api/v1/users/me/reward-points').as('rewardPoints');

        login();

        cy.visit('/#/profile');
        cy.contains(/Prämienpunkte/i, {timeout: 10000}).click({force: true});

        cy.wait('@rewardPoints', {timeout: 15000}).then(rp => {
            const pts = (rp.response && rp.response.body && rp.response.body.rewardPoints) || 0;
            cy.contains(/aktueller punktestand/i, {timeout: 10000}).should('exist');
            cy.contains(String(pts), {timeout: 10000}).should('exist');
        });

        cy.contains('button', 'ZU DEN PRÄMIEN', {timeout: 10000})
            .should('be.visible')
            .click({force: true});

        cy.location('hash', {timeout: 10000}).should('match', /merchandise\/rewards/i);
    });
});
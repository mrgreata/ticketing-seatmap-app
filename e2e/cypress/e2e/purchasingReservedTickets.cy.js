describe('User buys reserved tickets via cart (E2E)', () => {

    const dismissToasts = () => {
        cy.get('body').then($body => {
            const hasToastMsg = $body.find('#toast-container .toast-message').length > 0;
            if (!hasToastMsg) return;

            cy.get('#toast-container .toast-message')
                .click({ multiple: true, force: true });

            cy.get('body').then($b2 => {
                if ($b2.find('#toast-container .toast-close-button').length) {
                    cy.get('#toast-container .toast-close-button')
                        .click({ multiple: true, force: true });
                }
            });

            cy.get('#toast-container .toast-message', { timeout: 10000 }).should('not.exist');
        });
    };

    const fillInputByLabel = (labelText, value) => {
        cy.contains('label.form-label', labelText)
            .should('be.visible')
            .parent()
            .find('input.form-control')
            .clear()
            .type(value);
    };

    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/reservations/my').as('getReservations');
        cy.intercept('GET', '**/tickets/my').as('getMyTickets');

        cy.intercept('POST', '**/cart/tickets').as('addTicketsToCart');
        cy.intercept('GET', '**/cart').as('getCart');
        cy.intercept('POST', '**/cart/checkout').as('checkoutCart');
    });

    it('logs in and adds reservation to cart and cheks out successfully', () => {
        cy.log('--- Login Start ---');
        cy.visit('/#/login');

        cy.get('input[formcontrolname="email"]').type('userForPurchaseTest@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();

        cy.wait('@login').then(interception => {
            cy.log(`Login Response Status: ${interception.response.statusCode}`);
            console.log('Login Response:', interception.response.body);
        });

        dismissToasts();
        cy.log('--- Navigiere zu Tickets ---');
        cy.contains('span', 'Tickets', { timeout: 10000 }).click();

        cy.wait('@getReservations').its('response.statusCode').should('eq', 200);
        cy.wait('@getMyTickets').its('response.statusCode').should('eq', 200);

        cy.contains('h2', 'Reservierte Tickets', { timeout: 10000 }).should('exist');

        cy.get('div.ticket-group-card')
            .first()
            .find('div.ticket-row input[type="checkbox"]')
            .first()
            .check({ force: true });

        cy.contains('button', 'Ticket(s) zum Warenkorb hinzufügen', { timeout: 10000 })
            .should('be.visible')
            .click();
        dismissToasts();

        cy.wait('@addTicketsToCart').then(({ request, response }) => {
            expect(response && response.statusCode).to.be.oneOf([200, 201]);
            expect(request.body).to.be.an('array');
            expect(request.body.length).to.be.greaterThan(0);
            request.body.forEach(x => expect(x).to.satisfy(v => typeof v === 'number'));
        });

        cy.location('hash', { timeout: 10000 }).should('contain', '/cart');

        cy.wait('@getCart').its('response.statusCode').should('eq', 200);

        cy.contains('span.badge', 'Ticket', { timeout: 10000 }).should('exist');

        cy.contains('button', 'Zur Kassa', { timeout: 10000 })
            .should('be.visible')
            .should('not.be.disabled')
            .click();

        dismissToasts();

        cy.location('hash', { timeout: 10000 }).should('contain', '/checkout');

        cy.wait('@getCart').its('response.statusCode').should('eq', 200);

        fillInputByLabel('Kartennummer (16 Ziffern)', '4242424242424242');
        fillInputByLabel('Ablaufdatum (MMYY)', '1230');
        fillInputByLabel('CVC (3 Ziffern)', '123');

        cy.contains('button', 'Zahlungspflichtig bestellen', { timeout: 10000 })
            .should('be.visible')
            .click();

        cy.contains('h2', 'Rechnung herunterladen', { timeout: 10000 }).should('be.visible');

        cy.contains('button', 'Bestellung abschließen', { timeout: 10000 })
            .should('be.visible')
            .click();

        cy.wait('@checkoutCart').its('response.statusCode').should('be.oneOf', [200, 201]);

        cy.location('hash', { timeout: 10000 }).should('match', /#\/?$/);
        cy.contains('h1', /^Willkommen/).should('be.visible');
    });

});
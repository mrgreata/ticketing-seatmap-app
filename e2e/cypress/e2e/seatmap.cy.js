describe('Seatmap (E2E)', () => {
    const user = { email: 'user5@email.com', pw: 'password' };

    beforeEach(() => {
        cy.loginUI(user.email, user.pw);
    });

    it('lädt Seatmap und Weiter ist disabled ohne Auswahl', () => {
        cy.getEventIdWithFreeSeat().then((eventId) => {
            cy.intercept('GET', '**/api/v1/events/*/seatmap*').as('seatmap');
            cy.visitSeatmapWithEvent(eventId);
            cy.wait('@seatmap', { timeout: 15000 });

            cy.get('[data-cy=seat]', { timeout: 15000 }).should('have.length.greaterThan', 0);

            cy.get('[data-cy=continue]', { timeout: 10000 })
                .should('exist')
                .and('be.disabled');
        });
    });

    it('toggle: free -> selected -> free', () => {
        cy.getEventIdWithFreeSeat().then((eventId) => {
            cy.intercept('GET', '**/api/v1/events/*/seatmap*').as('seatmap');

            cy.visitSeatmapWithEvent(eventId);
            cy.wait('@seatmap', { timeout: 15000 });

            cy.get('[data-cy=seat][data-status="free"]', { timeout: 10000 })
                .its('length').should('be.gt', 0);

            cy.get('[data-cy=seat][data-status="free"]').first().click();
            cy.get('[data-cy=selected-count]').should('contain', '1');

            cy.get('[data-cy=seat][data-status="selected"]').first().click();
            cy.get('[data-cy=selected-count]').should('contain', '0');
        });
    });

    it('sold/reserved sind nicht auswählbar', () => {
        cy.getEventIdWithFreeSeat().then((eventId) => {
            cy.intercept('GET', '**/api/v1/events/*/seatmap*').as('seatmap');

            cy.visitSeatmapWithEvent(eventId);
            cy.wait('@seatmap', { timeout: 15000 });

            cy.get('body').then(($body) => {
                const hasReserved = $body.find('[data-cy=seat][data-status="reserved"]').length > 0;
                const hasSold = $body.find('[data-cy=seat][data-status="sold"]').length > 0;

                if (!hasReserved && !hasSold) {
                    cy.log('Kein reserved/sold Seat vorhanden → Test wird übersprungen');
                    return;
                }

                if (hasReserved) {
                    cy.get('[data-cy=seat][data-status="reserved"]').first().click({ force: true });
                    cy.get('[data-cy=selected-count]').should('contain', '0');
                }

                if (hasSold) {
                    cy.get('[data-cy=seat][data-status="sold"]').first().click({ force: true });
                    cy.get('[data-cy=selected-count]').should('contain', '0');
                }
            });
        });
    });

    it('Weiter erstellt Tickets und navigiert zu /#/cart', () => {
        cy.getEventIdWithFreeSeat().then((eventId) => {
            cy.intercept('GET', '**/api/v1/events/*/seatmap*').as('seatmap');
            cy.intercept('POST', '**/api/v1/tickets').as('createTickets');

            cy.visitSeatmapWithEvent(eventId);
            cy.url().should('include', '/#/seatmap');
            cy.wait('@seatmap', { timeout: 30000 });

            cy.get('[data-cy=seat][data-status="free"]', { timeout: 15000 })
                .should('have.length.greaterThan', 0);

            const attempt = (idx) => {
                return cy.get('[data-cy=seat][data-status="free"]').then($free => {
                    const n = $free.length;
                    expect(n).to.be.greaterThan(0);

                    if (idx >= Math.min(n, 10)) {
                        throw new Error(`Kein buchbarer free-seat gefunden (free seats im DOM: ${n}).`);
                    }

                    cy.wrap($free.eq(idx)).click({ force: true });

                    cy.get('[data-cy=continue]', { timeout: 10000 })
                        .should('exist')
                        .and('not.be.disabled')
                        .click();

                    return cy.wait('@createTickets', { timeout: 30000 }).then(({ response }) => {
                        const status = response?.statusCode;

                        if (status === 409) {
                            cy.get('[data-cy=seat][data-status="selected"]').first().click({ force: true });
                            return attempt(idx + 1);
                        }

                        expect(status).to.be.oneOf([200, 201]);
                    });
                });
            };

            attempt(0);

            cy.url({ timeout: 30000 }).should('include', '/#/cart');
        });
    });


    it('alle free seats sind klickbar (toggle)', () => {
        cy.getEventIdWithFreeSeat().then((eventId) => {
            cy.intercept('GET', '**/api/v1/events/*/seatmap*').as('seatmap');
            cy.visitSeatmapWithEvent(eventId);
            cy.url().should('include', '/#/seatmap');
            cy.wait('@seatmap', { timeout: 15000 });

            cy.get('[data-cy=seat][data-status="free"]', { timeout: 10000 }).then($seats => {
                const n = $seats.length;
                expect(n, 'free seats').to.be.greaterThan(0);

                cy.wrap($seats).each($s => cy.wrap($s).click());

                cy.get('[data-cy=selected-count]').should('contain', String(n));

                cy.get('[data-cy=seat][data-status="selected"]').then($sel => {
                    expect($sel.length, 'selected seats after clicking all free').to.equal(n);
                });

                cy.get('[data-cy=seat][data-status="selected"]').each($s => cy.wrap($s).click());
                cy.get('[data-cy=selected-count]').should('contain', '0');
            });
        });
    });
});
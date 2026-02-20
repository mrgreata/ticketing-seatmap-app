describe('User cancels purchased tickets (E2E)', () => {

    const dismissToasts = () => {
        cy.get('body').then($body => {
            if ($body.find('#toast-container').length) {
                cy.get('#toast-container').click({ multiple: true, force: true });
            }
        });
    };

    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/tickets/my').as('getPurchasedTickets');
        cy.intercept('DELETE', '**/tickets', (req) => {
            req.continue((res) => {
                expect(res.statusCode).to.be.oneOf([200, 204]);
            });
        }).as('cancelTickets');
    });

    it('logs in and cancels the only purchased ticket', () => {
        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('userSinglePurchased@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login').its('response.statusCode').should('eq', 200);
        dismissToasts();

        cy.contains('span', 'Tickets', { timeout: 10000 }).click();
        cy.wait('@getPurchasedTickets').its('response.statusCode').should('eq', 200);

        cy.get('div.ticket-group-card', { timeout: 10000 }).first().as('targetTicket');

        cy.get('@targetTicket')
            .find('.ticket-top span')
            .invoke('text')
            .then(text => text.trim())
            .as('targetEventName');

        cy.get('@targetTicket')
            .find('input[type="checkbox"]')
            .check({ force: true })
            .should('be.checked');

        cy.contains('button', 'Ticket(s) stornieren', { timeout: 10000 })
            .should('be.visible')
            .should('be.enabled')
            .click();

        cy.get('.modal-backdrop-custom', { timeout: 10000 })
            .should('be.visible')
            .within(() => {
                cy.get('input[type="checkbox"]').uncheck({ force: true });
                cy.contains('button', 'Stornieren').click();
            });

        cy.wait('@cancelTickets').then(({ response, request }) => {
            expect(response && response.statusCode).to.be.oneOf([200, 204]);
            expect(request.body).to.be.an('array').and.not.be.empty;
        });

        cy.get('@targetEventName').then((name) => {
            cy.contains('.ticket-top span', name).should('not.exist');
        });

        cy.get('.no-tickets-banner').should('be.visible');
        cy.contains('Noch keine Tickets vorhanden').should('be.visible');
        cy.contains('Sobald du Tickets reservierst oder kaufst, erscheinen sie hier.').should('be.visible');
    });
});

describe('User cancels ticket reservations', () => {
    const dismissToasts = () => {
        cy.get('body').then($body => {
            const hasToast = $body.find('#toast-container').length > 0;
            if (hasToast) {
                cy.get('#toast-container').click({ multiple: true, force: true });
            }
        });
    };

    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/reservations/my').as('getReservations');
        cy.intercept('PATCH', '**/reservations/cancellation').as('cancelReservation');
    });

    it('cancels a ticket reservation and verifies seats are freed', () => {
        let reservationNumber;

        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('user4@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login');
        dismissToasts();
        cy.contains('span', 'Tickets', { timeout: 10000 }).click();
        cy.wait('@getReservations').then((interception) => {
            const tickets = interception.response.body;
            cy.log('Reservations from backend:', JSON.stringify(tickets));
            if (tickets && tickets.length > 0) {
                cy.log('First ticket ID:', tickets[0].id);
                cy.log('First ticket reservationId:', tickets[0].reservationId);
            }
        });


        cy.contains('h2', 'Reservierte Tickets', { timeout: 10000 }).should('exist');

        cy.get('.ticket-group-card').first().within(() => {
            cy.get('.field').filter(':contains("RES-")').invoke('text').then(text => {
                reservationNumber = text.trim();
                cy.log('Found reservation number:', reservationNumber);
            });

            cy.get('input[type="checkbox"]').first().check({ force: true });

            cy.get('.ticket-info').first().invoke('attr', 'data-ticket-id').then(id => {
                cy.log('Ticket ID from UI:', id);
            });
        });

        cy.contains('button', 'Reservierung(en) stornieren')
            .should('be.enabled')
            .click();

        cy.on('window:confirm', () => true);

        cy.wait('@cancelReservation').then(({ request, response }) => {
            cy.log('Request body:', JSON.stringify(request.body));
            cy.log('Response:', JSON.stringify(response));

            console.log('CANCEL REQUEST', request.body);
            console.log('CANCEL RESPONSE', response);

            expect(response).to.not.be.undefined;
        });


        cy.contains('storniert', { timeout: 10000, matchCase: false }).should('exist');

        cy.get('#toast-container .toast-success', { timeout: 10000 }).should('exist');

        cy.wait('@getReservations');


        cy.get('body').then($body => {
            const hasNoTicketsBanner = $body.find('.no-tickets-banner').length > 0;
            const stillHasReservation = $body.text().includes(reservationNumber);

            if (hasNoTicketsBanner) {
                cy.log('Banner "Noch keine Tickets vorhanden" wird angezeigt - korrekt nach Stornierung');
                cy.get('.no-tickets-banner').should('be.visible');
                cy.contains('h2', 'Reservierte Tickets').should('not.exist');
            } else if (stillHasReservation) {
                cy.log('ERROR: Reservation still exists after cancellation:', reservationNumber);
                cy.contains(reservationNumber).should('not.exist');
            } else {
                cy.log('Reservation was successfully removed, no banner shown');
                cy.contains(reservationNumber).should('not.exist');
            }
        });
    });
});
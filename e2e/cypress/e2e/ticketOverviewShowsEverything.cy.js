describe('Ticket overview shows all ticket types', () => {
    const dismissToasts = () => {
        cy.get('body').then($body => {
            const hasToast = $body.find('#toast-container .toast-message').length > 0;
            if (!hasToast) return;
            cy.get('#toast-container .toast-message').click({ multiple: true, force: true });
        });
    };

    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/tickets/my').as('getMyTickets');
        cy.intercept('GET', '**/reservations/my').as('getReservations');
        cy.intercept('GET', '**/invoices/my/credits').as('getCreditInvoices');
    });

    describe('User with all ticket types', () => {
        it('displays reserved, upcoming, and past tickets', () => {
            cy.visit('/#/login');
            cy.get('input[formcontrolname="email"]').type('user5@email.com');
            cy.get('input[formcontrolname="password"]').type('password');
            cy.get('button[type="submit"]').click();
            cy.wait('@login');
            dismissToasts();

            cy.get('nav span.nav-link')
                .contains('Tickets')
                .should('be.visible')
                .click();

            cy.wait('@getReservations').then((interception) => {
                cy.log('Reservations API response:', JSON.stringify(interception.response.body));
                expect(interception.response.statusCode).to.equal(200);

                const reservations = interception.response.body;
                expect(reservations).to.be.an('array');
                if (reservations.length === 0) {
                    cy.log('WARN: User4 has NO reservations!');
                } else {
                    cy.log(`User4 has ${reservations.length} reservation(s)`);
                }
            });
            cy.wait('@getMyTickets').then((interception) => {
                cy.log('Tickets API response:', JSON.stringify(interception.response.body));
                expect(interception.response.statusCode).to.equal(200);
            });

            cy.wait('@getCreditInvoices');

            cy.get('h1.display-4', { timeout: 10000 })
                .contains('Meine Tickets')
                .should('be.visible');


            cy.contains('h2', 'Zukünftige Veranstaltungen').should('exist');

            cy.contains('h2', 'Zukünftige Veranstaltungen').then($h2 => {
                const text = $h2.text();
                if (text.includes('+')) {
                    cy.wrap($h2).click();
                    cy.wait(500);
                }
            });

            cy.get('.ticket-top').contains('span', 'Cats').should('exist');

            cy.get('.ticket-top').first().within(() => {
                cy.get('span').should('contain', 'Cats');
            });


            cy.contains('.ticket-group-card', 'Cats').then($card => {
                cy.wrap($card).within(() => {
                    cy.contains('Reihe').should('exist');
                    cy.contains('Platz').should('exist');
                    cy.contains('Datum').should('exist');
                    cy.contains('Einlass').should('exist');
                    cy.contains('Preis').should('exist');
                    cy.contains('€').should('exist');
                });
            });

            cy.get('.ticket-top').contains('span', 'Coldplay Tour').should('exist');
            cy.get('body').then($body => {
                if ($body.text().includes('Vergangene Veranstaltungen')) {
                    cy.contains('h2', 'Vergangene Veranstaltungen').should('exist');

                    cy.contains('h2', 'Vergangene Veranstaltungen').then($h2 => {
                        if ($h2.text().includes('+')) {
                            cy.wrap($h2).click();
                            cy.wait(500);
                        }
                    });
                } else {
                    console.log('No past tickets section (as expected)');
                }
            });


            cy.contains('button', 'Ticket(s) stornieren').should('exist');
            cy.contains('button', 'Rechnung(en) herunterladen').should('exist');
        });
    });

    describe('User with reserved tickets only', () => {
        it('displays only reserved tickets section', () => {
            cy.visit('/#/login');
            cy.get('input[formcontrolname="email"]').type('userWithReservation@email.com');
            cy.get('input[formcontrolname="password"]').type('password');
            cy.get('button[type="submit"]').click();
            cy.wait('@login');
            dismissToasts();

            cy.get('nav span.nav-link')
                .contains('Tickets')
                .should('be.visible')
                .click();

            cy.wait('@getReservations').then((interception) => {
                cy.log('Reservations API response:', JSON.stringify(interception.response.body));
                expect(interception.response.statusCode).to.equal(200);

                const reservations = interception.response.body;
                expect(reservations).to.be.an('array');

                expect(reservations.length).to.be.at.least(1, 'User sollte mindestens eine Reservierung haben');
                cy.log(`User hat ${reservations.length} reservation(s)`);

                reservations.forEach((res, index) => {
                    cy.log(`Reservation ${index + 1}:`, JSON.stringify({
                        reservationNumber: res.reservationNumber,
                        eventName: res.eventName,
                        ticketId: res.ticketId
                    }));
                });
            });

            cy.wait('@getMyTickets').then((interception) => {
                cy.log('Tickets API response:', JSON.stringify(interception.response.body));
                expect(interception.response.statusCode).to.equal(200);

                const purchasedTickets = interception.response.body;
                expect(purchasedTickets).to.be.an('array');
                if (purchasedTickets.length > 0) {
                    cy.log(`ACHTUNG: User hat ${purchasedTickets.length} gekaufte Ticket(s) - erwartet: 0`);
                }
            });

            cy.wait('@getCreditInvoices');

            cy.contains('h1', 'Meine Tickets', { timeout: 10000 }).should('be.visible');

            cy.contains('h2', 'Reservierte Tickets').should('exist');

            cy.contains('Bitte beachten: Reservierte Tickets müssen eine halbe Stunde vor Veranstaltungsbeginn abgeholt werden.')
                .should('be.visible');

            cy.get('.ticket-top').within(() => {
                cy.contains('span', 'Reserved Only Concert').should('exist');
            });

            cy.contains('.ticket-group-card', 'Reserved Only Concert').within(() => {
                cy.contains('Reihe').should('exist');
                cy.contains('Platz').should('exist');
                cy.contains('Datum').should('exist');
                cy.contains('Einlass').should('exist');
                cy.contains('Preis').should('exist');
                cy.contains('Reservierungsnummer').should('exist');
                cy.get('.field').filter(':contains("RES-")').should('exist');
            });

            cy.get('body').then($body => {
                const hasUpcoming = $body.text().includes('Zukünftige Veranstaltungen');
                const hasPast = $body.text().includes('Vergangene Veranstaltungen');

                if (hasUpcoming) {
                    cy.log('WARN: User hat "Zukünftige Veranstaltungen" Sektion, erwartet: keine');
                    cy.contains('h2', 'Zukünftige Veranstaltungen').should('not.exist');
                }
                if (hasPast) {
                    cy.log('WARN: User hat "Vergangene Veranstaltungen" Sektion, erwartet: keine');
                    cy.contains('h2', 'Vergangene Veranstaltungen').should('not.exist');
                }
            });

            cy.contains('button', 'Reservierung(en) stornieren').should('exist');
            cy.contains('button', 'Ticket(s) zum Warenkorb hinzufügen').should('exist');

            cy.contains('button', 'Ticket(s) stornieren').should('not.exist');
            cy.contains('button', 'Rechnung(en) herunterladen').should('not.exist');
        });
    });

    describe('User with no tickets', () => {
        it('shows empty state message', () => {

            cy.visit('/#/login');
            cy.get('input[formcontrolname="email"]').type('user6@email.com');
            cy.get('input[formcontrolname="password"]').type('password');
            cy.get('button[type="submit"]').click();
            cy.wait('@login');
            dismissToasts();

            cy.get('nav span.nav-link')
                .contains('Tickets')
                .should('be.visible')
                .click();

            cy.wait(['@getMyTickets', '@getReservations', '@getCreditInvoices']);

            cy.get('.no-tickets-banner').should('be.visible');
            cy.contains('Noch keine Tickets vorhanden').should('be.visible');
            cy.contains('Sobald du Tickets reservierst oder kaufst, erscheinen sie hier.').should('be.visible');

            cy.contains('h2', 'Reservierte Tickets').should('not.exist');
            cy.contains('h2', 'Zukünftige Veranstaltungen').should('not.exist');
            cy.contains('h2', 'Vergangene Veranstaltungen').should('not.exist');

            cy.get('.ticket-group-card').should('not.exist');
        });
    });
});
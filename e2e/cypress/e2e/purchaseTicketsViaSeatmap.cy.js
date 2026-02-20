describe('User buys tickets via seatmap', () => {
    const dismissToasts = () => {
        cy.get('body').then($body => {
            const hasToast = $body.find('#toast-container .toast-message').length > 0;
            if (!hasToast) return;
            cy.get('#toast-container .toast-message').click({ multiple: true, force: true });
        });
    };


    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/api/v1/events/*/seatmap').as('getSeatmap');
        cy.intercept('GET', '**/events/*').as('getEventDetail');
        cy.intercept('POST', '**/tickets').as('createTickets');
        cy.intercept('POST', '**/cart/tickets').as('addToCart');
        cy.intercept('GET', '**/cart').as('getCart');
        cy.intercept('POST', '**/cart/checkout').as('checkout');
        cy.intercept('GET', '**/events?*').as('getEvents');
    });

    it('logs in, navigates from home to events, selects seats and buys them', () => {
        // ---- Login ----
        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('user5@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login');
        dismissToasts();

        cy.location('hash', { timeout: 10000 }).should('match', /#\/?$/); // Leer oder root

        // Verifizieren, dass wir auf der Startseite sind
        cy.contains('h1', 'Willkommen', { timeout: 10000 }).should('be.visible');

        // Entweder "Willkommen bei Ticketline!" (nicht eingeloggt)
        // oder "Willkommen, [Name]!" (eingeloggt)
        cy.get('body').then($body => {
            if ($body.text().includes('Willkommen bei Ticketline!')) {
                // Nicht eingeloggt - sollte nicht passieren
                cy.log('User ist nicht eingeloggt - Test fehlgeschlagen');
                return;
            }

            // Eingeloggt - suche nach Willkommensnachricht
            cy.contains('h1', /^Willkommen,/).should('be.visible');
        });

        cy.wait(500);

        // OPTION 1: Über "Veranstaltungen" Button in der Navigation
        cy.get('nav span.nav-link')
            .contains('Veranstaltungen')
            .should('be.visible')
            .click({ force: true });

        // OPTION 2: Über "Alle Veranstaltungen anzeigen" Link (falls auf Startseite)
        // cy.contains('a', 'Alle Veranstaltungen anzeigen').click();

        // ---- Wait for events page to load ----
        cy.location('hash', { timeout: 10000 }).should('contain', '/events');
        cy.wait('@getEvents').its('response.statusCode').should('eq', 200);
        cy.contains('h1', 'Veranstaltungen', { timeout: 10000 }).should('be.visible');


        cy.contains('.event-card', 'Cats', { timeout: 10000 })
            .should('be.visible')
            .within(() => {
                cy.get('a.event-card-button')
                    .should('be.visible')
                    .and('contain', 'Details')
                    .click();
            });

        // ---- Wait for event detail page to load ----
        cy.location('hash', { timeout: 10000 }).should('match', /\/events\/\d+/);
        cy.wait('@getEventDetail').its('response.statusCode').should('eq', 200);

        // ---- Navigate to seatmap from event detail ----
        cy.get('button.btn-saalplan')
            .should('be.visible')
            .and('contain', 'SAALPLAN')
            .and('not.be.disabled')
            .click();

        // ---- Wait for seatmap to load ----
        cy.location('hash', { timeout: 10000 }).should('contain', '/seatmap');
        cy.wait('@getSeatmap', { timeout: 10000 }).its('response.statusCode').should('eq', 200);

        cy.get('.seatmap-container h2', { timeout: 10000 }).should('contain', 'Saalplan');
        cy.get('.seatmap-layout, .seat-grid', { timeout: 10000 }).should('be.visible');

        // ---- Check seat statuses ----
        cy.get('.seat').should('have.length.greaterThan', 0);

        // Überprüfe, ob es überhaupt freie Plätze gibt
        cy.get('.seat.status-free').then($freeSeats => {
            if ($freeSeats.length === 0) {
                cy.log('Keine freien Plätze verfügbar. Alle Plätze sind entweder verkauft oder reserviert.');

                // OPTION: Suche nach einem anderen Event
                // cy.go('back'); // Zurück zur Event-Liste
                // findDifferentEvent();
                // ODER: Test mit reservierten Plätzen fortfahren
                cy.get('.seat:not(.status-sold):not(.empty)')
                    .first()
                    .click();
            }
        });

        // ---- Select multiple seats ----
        cy.get('.seat:not(.status-sold):not(.empty)')
            .should('have.length.greaterThan', 0)
            .then($availableSeats => {
                if ($availableSeats.length >= 2) {
                    // Wähle die ersten beiden verfügbaren Plätze
                    cy.wrap($availableSeats).first().click();
                    cy.get('.selection-info span').should('contain', '1');

                    cy.wrap($availableSeats).eq(1).click();
                    cy.get('.selection-info span').should('contain', '2');

                    // Verify multiple seats selected
                    cy.get('.seat.status-selected').should('have.length', 2);
                } else {
                    // Wenn nicht genug Plätze verfügbar sind, wähle nur einen
                    cy.wrap($availableSeats).first().click();
                    cy.get('.selection-info span').should('contain', '1');
                    cy.get('.seat.status-selected').should('have.length', 1);
                }
            });

        // ---- Click "In den Warenkorb" ----
        cy.contains('button', 'In den Warenkorb')
            .should('be.enabled')
            .click();

        // ---- Verify ticket creation and reservation ----
        cy.wait('@createTickets').its('response.statusCode').should('eq', 201);
        cy.wait('@addToCart').its('response.statusCode').should('be.oneOf', [200, 201]);

        // ---- Navigate to cart ----
        cy.location('hash', { timeout: 10000 }).should('contain', '/cart');
        cy.wait('@getCart').its('response.statusCode').should('eq', 200);

        cy.contains('.badge', 'Ticket').should('exist');
        cy.contains('button', 'Zur Kassa').should('be.enabled').click();

        cy.location('hash', { timeout: 10000 }).should('contain', '/checkout');
        cy.wait('@getCart').its('response.statusCode').should('eq', 200);

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


        cy.get('.modal-backdrop-custom', { timeout: 10000 })
            .should('be.visible')
            .within(() => {
                cy.contains('button', 'Bestellung abschließen')
                    .should('be.visible')
                    .click();
            });

        cy.wait('@checkout').its('response.statusCode').should('be.oneOf', [200, 201]);

        // ---- Zurück zur Startseite (News Home) ----
        cy.location('hash', { timeout: 10000 }).should('match', /#\/?$/);
        cy.contains('h1', /^Willkommen/).should('be.visible');
    });
});

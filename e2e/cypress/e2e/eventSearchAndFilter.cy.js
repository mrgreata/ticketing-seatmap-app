describe('Event Search and Filter (E2E)', () => {

    const dismissToasts = () => {
        cy.get('body').then($body => {
            if ($body.find('.ngx-toastr').length > 0) {
                cy.get('.ngx-toastr').invoke('remove');
            }
        });
    };

    const focusSearchInput = () => {
        const sel = 'input[placeholder="Nach Veranstaltung suchen..."]';

        cy.get(sel, { timeout: 10000 })
            .should('exist')
            .scrollIntoView({ block: 'center', inline: 'nearest' })
            .should('be.visible');

        cy.get('header.main-header', { timeout: 10000 }).then($header => {
            const headerH = $header.length ? $header[0].getBoundingClientRect().height : 0;

            cy.window().then(win => {
                win.scrollBy(0, -(headerH + 16));
            });
        });

        cy.get(sel)
            .should('be.visible')
            .click({ scrollBehavior: false });
    };

    const clickSearchButtonSafely = () => {
        cy.contains('button', 'Suchen', { timeout: 10000 })
            .should('be.visible')
            .scrollIntoView({ block: 'center', inline: 'nearest' });

        cy.get('header.main-header').then($header => {
            const headerH = $header.length ? $header[0].getBoundingClientRect().height : 0;

            cy.window().then(win => {
                win.scrollBy(0, -(headerH + 16));
            });
        });

        cy.contains('button', 'Suchen')
            .should('be.visible')
            .click({ scrollBehavior: false });
    };


    beforeEach(() => {
        cy.intercept('POST', '**/authentication').as('login');
        cy.intercept('GET', '**/api/v1/events**').as('getEvents');
        cy.intercept('POST', '**/api/v1/events').as('createEvent');
        cy.intercept('DELETE', '**/api/v1/events/*').as('deleteEvent');
        cy.intercept('GET', '**/api/v1/locations').as('getLocations');
        cy.intercept('GET', '**/api/v1/artists').as('getArtists');
    });

    it('user searches for existing event and applies filters', () => {
        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('user5@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login').its('response.statusCode').should('eq', 200);
        dismissToasts();

        cy.contains('Veranstaltungen', { timeout: 10000 }).click();
        cy.wait('@getEvents').its('response.statusCode').should('eq', 200);
        cy.url().should('include', '/events');

        cy.contains('.event-card-title', 'Cats', { timeout: 10000 }).should('be.visible');
        cy.contains('.event-card-title', 'Coldplay Tour', { timeout: 10000 }).should('be.visible');
        cy.contains('.event-card-title', 'Taylor Swift', { timeout: 10000 }).should('be.visible');

        cy.get('input[placeholder="Nach Veranstaltung suchen..."]').clear().type('Cats');
        cy.contains('button', 'Suchen').click();

        cy.contains('.event-card-title', 'Cats', { timeout: 10000 }).should('be.visible');
        cy.contains('.event-card-title', 'Coldplay Tour').should('not.exist');
        cy.contains('.event-card-title', 'Taylor Swift').should('not.exist');

        cy.contains('button', 'Zurücksetzen').click();
        cy.wait(1000);
        cy.contains('.event-card-title', 'Coldplay Tour', { timeout: 10000 }).should('be.visible');

        cy.contains('button.mode-btn', 'Künstler').click();

        cy.get('input[placeholder="Nach Künstler suchen..."]').clear().type('Taylor');
        cy.contains('button', 'Suchen').click();
        cy.wait(1000);

        cy.contains('.result-item', 'Taylor Swift').should('be.visible').click();
        cy.wait(1000);

        cy.contains('.event-card-title', 'Taylor Swift', { timeout: 10000 }).should('be.visible');
        cy.contains('.event-card-title', 'Cats').should('not.exist');
    });

    it('validates filter fields with inline errors', () => {
        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('user5@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login').its('response.statusCode').should('eq', 200);
        dismissToasts();

        cy.contains('Veranstaltungen', { timeout: 10000 }).click();
        cy.wait('@getEvents').its('response.statusCode').should('eq', 200);

        cy.contains('button', 'Filter').click();

        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const afterTomorrow = new Date();
        afterTomorrow.setDate(afterTomorrow.getDate() + 2);

        cy.get('input[type="date"]').eq(0).invoke('removeAttr', 'min');
        cy.get('input[type="date"]').eq(1).invoke('removeAttr', 'min');

        cy.get('input[type="date"]').eq(0)
            .invoke('val', afterTomorrow.toISOString().split('T')[0])
            .trigger('input')
            .trigger('change');

        cy.get('input[type="date"]').eq(1)
            .invoke('val', tomorrow.toISOString().split('T')[0])
            .trigger('input')
            .trigger('change');

        cy.get('label').contains('Veranstaltungstyp').click();

        cy.wait(1000);

        cy.get('.validation-error').contains('darf nicht nach').should('be.visible');

        cy.get('input[type="number"]').eq(1)
            .clear()
            .type('100')
            .trigger('input')
            .trigger('change');

        cy.get('input[type="number"]').eq(2)
            .clear()
            .type('50')
            .trigger('input')
            .trigger('change');

        cy.get('label').contains('Von Datum').click();

        cy.wait(1000);

        cy.get('.validation-error').contains('größer').should('be.visible');

        cy.contains('button.mode-btn', 'Ort').click();
        cy.wait(500);

        cy.contains('label', 'PLZ')
            .parent()
            .find('input')
            .should('be.visible')
            .clear()
            .type('12')
            .trigger('input');

        cy.wait(1000);

        cy.get('.validation-error').contains('genau 4 Ziffern').should('be.visible');

        cy.contains('label', 'PLZ')
            .parent()
            .find('input')
            .clear()
            .type('1010')
            .trigger('input');

        cy.wait(1000);

        cy.get('.validation-error').contains('genau 4 Ziffern').should('not.exist');
    });

    it('admin creates and deletes a test event', () => {
        const testTitle = `E2E Admin Test ${Date.now()}`;

        cy.visit('/#/login');
        cy.get('input[formcontrolname="email"]').type('admin@email.com');
        cy.get('input[formcontrolname="password"]').type('password');
        cy.get('button[type="submit"]').click();
        cy.wait('@login').its('response.statusCode').should('eq', 200);
        dismissToasts();

        cy.contains('button', 'Admin', { timeout: 10000 }).click();
        cy.contains('a', 'Veranstaltung erstellen').click();
        cy.url().should('include', '/events/new');

        cy.wait('@getLocations').its('response.statusCode').should('eq', 200);
        cy.wait('@getArtists').its('response.statusCode').should('eq', 200);

        cy.get('input[formcontrolname="title"]').type(testTitle);
        cy.get('select[formcontrolname="type"]').select('Konzert');
        cy.get('input[formcontrolname="durationMinutes"]').clear().type('120');
        cy.get('textarea[formcontrolname="description"]').type('Automatisch erstelltes Test-Event');

        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(19, 0, 0, 0);
        const dateStr = tomorrow.toISOString().slice(0, 16);
        cy.get('input[formcontrolname="dateTime"]').clear().type(dateStr);

        cy.get('select[formcontrolname="locationId"]').select(1);

        cy.get('button[type="submit"]').should('not.be.disabled').click();
        cy.wait('@createEvent').its('response.statusCode').should('eq', 201);
        dismissToasts();

        cy.url().should('match', /\/#\/events\/\d+/);

        cy.contains('button', 'Zurück zur Übersicht', { timeout: 10000 }).click();
        cy.url().should('match', /\/#\/events$/);
        cy.wait('@getEvents').its('response.statusCode').should('eq', 200);

        focusSearchInput();
        cy.get('input[placeholder="Nach Veranstaltung suchen..."]').clear().type(testTitle);
        clickSearchButtonSafely();
        cy.wait(1000);

        cy.contains('.event-card-title', testTitle, { timeout: 10000 }).should('be.visible');

        cy.contains('.event-card', testTitle)
            .find('.admin-icon-delete')
            .click();

        cy.get('.modal:visible', { timeout: 10000 })
            .should('be.visible')
            .within(() => {
                cy.contains('button', 'Ja, löschen').should('be.visible').click();
            });

        cy.wait('@deleteEvent').its('response.statusCode').should('be.oneOf', [200, 204]);
        dismissToasts();
        cy.wait('@getEvents').its('response.statusCode').should('eq', 200);

        cy.get('.modal:visible', { timeout: 5000 }).should('not.exist');
        cy.get('body').then($body => {
            if ($body.find('.modal-backdrop').length > 0) {
                cy.get('.modal-backdrop').invoke('remove');
            }
        });

        cy.wait(500);

        focusSearchInput();
        cy.get('input[placeholder="Nach Veranstaltung suchen..."]').clear().type(testTitle);
        clickSearchButtonSafely();
        cy.wait(1000);

        cy.contains('.event-card-title', testTitle).should('not.exist');
    });
});
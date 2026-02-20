Cypress.Commands.add('loginAdmin', () => {
    cy.fixture('settings').then(settings => {
        cy.visit(settings.baseUrl);
        cy.contains('a', 'Login').click();
        cy.get('input[name="username"]').type(settings.adminUser);
        cy.get('input[name="password"]').type(settings.adminPw);
        cy.contains('button', 'Login').click();
    })
})

Cypress.Commands.add('createMessage', (msg) => {
    cy.fixture('settings').then(settings => {
        cy.contains('a', 'Message', { timeout: 15000 })
            .should('be.visible')
            .click();
        cy.contains('button', 'Add message').click();
        cy.get('input[name="title"]').type('title' +  msg);
        cy.get('textarea[name="summary"]').type('summary' +  msg);
        cy.get('textarea[name="text"]').type('text' +  msg);
        cy.get('button[id="add-msg"]').click();
        cy.get('button[id="close-modal-btn"]').click();

        cy.contains('title' +  msg).should('be.visible');
        cy.contains('summary' +  msg).should('be.visible');
    })
})

Cypress.Commands.add('loginUI', (email, password) => {
    cy.intercept('POST', '**/api/v1/authentication*').as('auth');

    cy.visit('/#/login');

    cy.get('input[type="email"]', { timeout: 15000 })
        .should('be.visible')
        .clear()
        .type(email);

    cy.get('input[type="password"]', { timeout: 15000 })
        .should('be.visible')
        .clear()
        .type(password, { log: false });

    cy.get('button[type="submit"]', { timeout: 15000 })
        .should('be.enabled')
        .scrollIntoView()
        .click({ force: true });

    cy.wait('@auth', { timeout: 15000 }).then(({ response }) => {
        expect(response, 'auth response').to.exist;
        expect(response.statusCode).to.be.oneOf([200, 201]);
    });

    cy.url({ timeout: 15000 }).should('include', '/#/');
    cy.get('app-root', { timeout: 15000 }).should('be.visible');  // oder navbar selector
});



Cypress.Commands.add('getEventIdByTitle', (title) => {
    const needle = (title || '').toLowerCase();

    return cy.request('GET', '/api/v1/events').then((res) => {
        // 1) normalize -> events array
        let events = res.body;

        // Pageable: { content: [...] }
        if (events && !Array.isArray(events) && Array.isArray(events.content)) {
            events = events.content;
        }

        // Weird nested: [ [ ... ] ]
        if (Array.isArray(events) && events.length === 1 && Array.isArray(events[0])) {
            events = events[0];
        }

        expect(events, 'events list').to.be.an('array');
        expect(events, 'events list').not.to.be.empty;

        // 2) find by title-like fields
        const ev = events.find(e => {
            const t = (e.title ?? e.name ?? e.eventName ?? '').toLowerCase();
            return t.includes(needle);
        });

        if (!ev) {
            const titles = events.map(x => x.title ?? x.name ?? x.eventName ?? '(no title)').join(', ');
            throw new Error(`Event containing "${title}" nicht gefunden. Verfügbare Titel: ${titles}`);
        }

        // 3) id field might be id or eventId
        return ev.id ?? ev.eventId;
    });
});




Cypress.Commands.add('visitSeatmapWithEvent', (eventId) => {
    cy.visit(`/#/seatmap?eventId=${eventId}`);
});

Cypress.Commands.add('getAnyEventId', () => {
    return cy.request('GET', '/api/v1/events').then((res) => {
        let events = res.body;

        if (events && !Array.isArray(events) && Array.isArray(events.content)) {
            events = events.content;
        }
        if (Array.isArray(events) && events.length === 1 && Array.isArray(events[0])) {
            events = events[0];
        }

        expect(events, 'events list').to.be.an('array').and.not.be.empty;
        const ev = events[0];

        return ev.id ?? ev.eventId;
    });
});

Cypress.Commands.add('getEventIdWithFreeSeat', () => {
    return cy.request('GET', '/api/v1/events').then((res) => {
        const events = Array.isArray(res.body) ? res.body : (res.body?.content ?? []);
        expect(events, 'events list').to.be.an('array').and.not.to.be.empty;

        const pick = (i) => {
            if (i >= events.length) {
                throw new Error('Kein Event mit free seats gefunden');
            }

            const ev = events[i];
            const id = ev.id ?? ev.eventId;
            expect(id, 'event id').to.exist;

            return cy.request('GET', `/api/v1/events/${id}/seatmap`).then((sm) => {
                const body = sm.body;
                const seats =
                    Array.isArray(body) ? body :
                        Array.isArray(body?.seats) ? body.seats :
                            Array.isArray(body?.seatmap) ? body.seatmap :
                                Array.isArray(body?.cells) ? body.cells :
                                    [];

                const hasFree = seats.some(s => (s?.status || '').toLowerCase() === 'free');
                return hasFree ? id : pick(i + 1);
            });
        };

        return pick(0);
    });
});




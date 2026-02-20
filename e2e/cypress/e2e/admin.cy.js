describe('Admin User E2E', () => {

  function dismissToasts() {
    cy.get('body').then($body => {
      if ($body.find('.ngx-toastr').length) {
        cy.get('.ngx-toastr', { timeout: 0 }).invoke('remove');
      }
    });
  }

  function dismissModals() {
    cy.get('body').then($body => {
      if ($body.find('.modal-backdrop').length > 0) {
        cy.get('.modal-backdrop').invoke('remove');
        cy.get('body').invoke('removeClass', 'modal-open');
      }

      if ($body.find('.modal.show').length > 0) {
        cy.get('.modal.show').invoke('removeClass', 'show');
      }
    });
  }


  it('admin can unlock a locked user via user management', () => {
    const email = `locked_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');
    cy.intercept('POST', '**/authentication').as('login');
    cy.intercept('GET', '**/api/v1/admin/users*').as('getUsers');
    cy.intercept('PATCH', '**/api/v1/admin/users/*/lock-state').as('updateLock');

    // ---------- Register user ----------
    cy.visit('/#/registration');
    cy.get('input[formcontrolname="firstName"]').type('Max');
    cy.get('input[formcontrolname="lastName"]').type('Mustermann');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('button[type="submit"]').click();
    cy.wait('@register').its('response.statusCode').should('eq', 201);

    // ---------- Lock user via failed logins ----------
    cy.visit('/#/login');
    for (let i = 0; i < 5; i++) {
      cy.get('input[formcontrolname="email"]').clear().type(email);
      cy.get('input[formcontrolname="password"]').clear().type('wrongPassword');
      cy.get('button[type="submit"]').click();
      cy.wait('@login').its('response.statusCode').should('eq', i < 4 ? 401 : 423);
    }

    // ---------- Login as admin ----------
    cy.visit('/#/login');
    cy.get('input[formcontrolname="email"]').clear().type('admin@email.com');
    cy.get('input[formcontrolname="password"]').clear().type('password');
    cy.get('button[type="submit"]').click();
    cy.wait('@login').its('response.statusCode').should('eq', 200);

    dismissToasts();

    // ---------- Open user management ----------
    cy.contains('button', 'Admin').click();
    cy.contains('a', 'Benutzerverwaltung').click();
    cy.wait('@getUsers');

    // ---------- Switch to "Gesperrte Benutzer" ----------
    cy.contains('button', 'Gesperrte Benutzer').click();
    cy.wait('@getUsers');

    // ---------- Unlock user ----------
    cy.get('.search-pill input').clear().type(email);
    cy.wait('@getUsers');

    cy.contains('button', 'Entsperren').click();


    cy.get('.modal.show').should('be.visible').within(() => {
      cy.contains('button', 'Entsperren').click();
    });

    cy.wait('@updateLock').its('response.statusCode').should('be.oneOf', [200, 204]);
    cy.wait('@getUsers');

    // ---------- Verify user no longer locked ----------

    cy.contains('h2', 'Gesperrte Benutzer').should('be.visible');

    dismissModals()

    cy.get('.search-pill input').clear().type(email);
    cy.wait('@getUsers');


    cy.contains('Keine Benutzer gefunden.').should('be.visible');

    // ---------- Switch to "Aktive Benutzer" ----------

    dismissModals();

    cy.contains('button', 'Aktive Benutzer').click();
    cy.wait('@getUsers');

    cy.contains('h2', 'Aktive Benutzer').should('be.visible');

    cy.get('.search-pill input').clear().type(email);
    cy.wait('@getUsers');

    cy.get('table')
        .should('be.visible')
        .should('contain.text', email);
  });

  it('admin can create a new user and duplicate email is rejected', () => {
    const email = `created_${Date.now()}@test.com`;

    cy.intercept('POST', '**/authentication').as('login');
    cy.intercept('POST', '**/api/v1/admin/users').as('createUser');

    cy.visit('/#/login');
    cy.get('input[formcontrolname="email"]').type('admin@email.com');
    cy.get('input[formcontrolname="password"]').type('password');
    cy.get('button[type="submit"]').click();
    cy.wait('@login').its('response.statusCode').should('eq', 200);

    dismissToasts();

    cy.contains('button', 'Admin').click();
    cy.contains('a', 'Benutzer anlegen').click();

    cy.get('input[formcontrolname="firstName"]').type('Anna');
    cy.get('input[formcontrolname="lastName"]').type('Admin');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('select[formcontrolname="userRole"]').select('ROLE_USER');
    cy.get('button[type="submit"]').should('not.be.disabled').click();

    cy.wait('@createUser').its('response.statusCode').should('eq', 201);
    cy.get('.toast-success').should('be.visible').and('contain.text', 'erfolgreich');

    dismissToasts();

    cy.get('input[formcontrolname="firstName"]').type('Anna');
    cy.get('input[formcontrolname="lastName"]').type('Admin');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('select[formcontrolname="userRole"]').select('ROLE_USER');
    cy.get('button[type="submit"]').should('not.be.disabled').click();

    cy.wait('@createUser').its('response.statusCode').should('eq', 409);
    cy.get('.toast-error').should('be.visible').and('contain.text', 'E-Mail-Adresse');
  });

  it('admin can trigger a password reset for a locked user', () => {
    const email = `reset_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');
    cy.intercept('POST', '**/authentication').as('login');
    cy.intercept('GET', '**/api/v1/admin/users*').as('getUsers');
    cy.intercept('POST', '**/api/v1/admin/users/*/password-reset').as('passwordReset');

    // ---------- Register user ----------
    cy.visit('/#/registration');
    cy.get('input[formcontrolname="firstName"]').type('Reset');
    cy.get('input[formcontrolname="lastName"]').type('User');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('button[type="submit"]').click();
    cy.wait('@register').its('response.statusCode').should('eq', 201);

    // ---------- Lock user via failed logins ----------
    cy.visit('/#/login');
    for (let i = 0; i < 5; i++) {
      cy.get('input[formcontrolname="email"]').clear().type(email);
      cy.get('input[formcontrolname="password"]').clear().type('wrongPassword');
      cy.get('button[type="submit"]').click();
      cy.wait('@login').its('response.statusCode').should('eq', i < 4 ? 401 : 423);
    }

    // ---------- Login as admin ----------
    cy.visit('/#/login');
    cy.get('input[formcontrolname="email"]').clear().type('admin@email.com');
    cy.get('input[formcontrolname="password"]').clear().type('password');
    cy.get('button[type="submit"]').click();
    cy.wait('@login').its('response.statusCode').should('eq', 200);

    dismissToasts();

    // ---------- Open user management ----------
    cy.contains('button', 'Admin').click();
    cy.contains('a', 'Benutzerverwaltung').click();
    cy.wait('@getUsers');

    // ---------- Switch to locked users ----------
    cy.contains('button', 'Gesperrte Benutzer').click();
    cy.wait('@getUsers');

    cy.contains('h2', 'Gesperrte Benutzer').should('be.visible');

    // ---------- Search for user ----------
    cy.get('.search-pill input').clear().type(email);
    cy.wait('@getUsers');

    // ---------- Trigger password reset ----------
    cy.get('table')
        .should('be.visible')
        .within(() => {
          cy.contains('td', email)
              .parents('tr')
              .within(() => {
                cy.contains('button', 'Passwort zurücksetzen').click();
              });
        });

    // ---------- Confirm modal ----------
    cy.get('.modal.show')
        .should('be.visible')
        .within(() => {
          cy.contains('button', 'Passwort zurücksetzen').click();
        });

    // ---------- Assert backend call ----------
    cy.wait('@passwordReset')
        .its('response.statusCode')
        .should('be.oneOf', [200, 204]);

    // ---------- Success feedback ----------
    cy.get('.toast-success')
        .should('be.visible')
        .and('contain.text', 'Passwort');
  });

});
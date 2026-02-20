describe('Login E2E', () => {

  it('logs in an existing user successfully', () => {
    const email = `login_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');
    cy.intercept('POST', '**/authentication').as('login');

    cy.visit('/#/registration');

    cy.get('input[formcontrolname="firstName"]').type('Max');
    cy.get('input[formcontrolname="lastName"]').type('Mustermann');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');

    cy.get('button[type="submit"]').click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 201);

    cy.visit('/#/login');

    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('button[type="submit"]').click();

    cy.wait('@login')
    .its('response.statusCode')
    .should('eq', 200);

    cy.get('.toast-success').should('be.visible');
  });

  it('disables submit button for invalid client-side input', () => {
    cy.intercept('POST', '**/authentication').as('login');

    cy.visit('/#/login');

    cy.get('input[formcontrolname="email"]').type('invalid-email');
    cy.get('input[formcontrolname="password"]').type('123');

    cy.get('button[type="submit"]').should('be.disabled');
    cy.get('@login.all').should('have.length', 0);
  });

  it('rejects wrong password with 401', () => {
    const email = `wrongpw_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');
    cy.intercept('POST', '**/authentication').as('login');

    cy.visit('/#/registration');

    cy.get('input[formcontrolname="firstName"]').type('Max');
    cy.get('input[formcontrolname="lastName"]').type('Mustermann');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('button[type="submit"]').click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 201);

    cy.visit('/#/login');

    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('wrongPassword');
    cy.get('button[type="submit"]').click();

    cy.wait('@login')
    .its('response.statusCode')
    .should('eq', 401);

    cy.get('.toast-error').should('be.visible');
  });

  it('locks account after multiple failed login attempts', () => {
    const email = `locked_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');
    cy.intercept('POST', '**/authentication').as('login');

    cy.visit('/#/registration');

    cy.get('input[formcontrolname="firstName"]').type('Max');
    cy.get('input[formcontrolname="lastName"]').type('Mustermann');
    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('input[formcontrolname="password"]').type('password123');
    cy.get('button[type="submit"]').click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 201);

    cy.visit('/#/login');

    for (let i = 0; i < 5; i++) {
      cy.get('input[formcontrolname="email"]').clear().type(email);
      cy.get('input[formcontrolname="password"]').clear().type('wrongPassword');
      cy.get('button[type="submit"]').click();

      cy.wait('@login')
      .its('response.statusCode')
      .should(i < 4 ? 'eq' : 'eq', i < 4 ? 401 : 423);
    }

    cy.get('.toast-error').should('be.visible');
  });

  it('allows requesting a password reset from the login flow', () => {
    const email = `reset_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/password-reset/request').as('resetRequest');

    cy.visit('/#/login');

    cy.contains('Passwort vergessen').click();

    cy.url().should('include', '/password-reset/request');

    cy.get('input[formcontrolname="email"]').type(email);
    cy.get('button[type="submit"]').click();

    cy.wait('@resetRequest')
    .its('response.statusCode')
    .should('eq', 204);

    cy.contains('Falls ein Konto existiert').should('be.visible');
  });

});
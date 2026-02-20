describe('Registration E2E', () => {

  const fillRegistrationForm = (email, password = 'password123') => {
    cy.get('input[formcontrolname="firstName"]')
    .should('be.visible')
    .clear()
    .type('Max');

    cy.get('input[formcontrolname="lastName"]')
    .clear()
    .type('Mustermann');

    cy.get('input[formcontrolname="email"]')
    .clear()
    .type(email);

    cy.get('input[formcontrolname="password"]')
    .clear()
    .type(password);
  };

  it('registers a new user successfully', () => {
    const email = `e2e_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');

    cy.visit('/#/registration');

    fillRegistrationForm(email);

    cy.get('button[type="submit"]')
    .should('not.be.disabled')
    .click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 201);

    cy.get('.toast-success').should('be.visible');
  });

  it('disables submit button for weak password on client side', () => {
    const email = `weak_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');

    cy.visit('/#/registration');

    fillRegistrationForm(email, '123');

    cy.get('button[type="submit"]').should('be.disabled');
    cy.get('@register.all').should('have.length', 0);
  });

  it('rejects duplicate email with 409', () => {
    const email = `dup_${Date.now()}@test.com`;

    cy.intercept('POST', '**/users/registration').as('register');

    cy.visit('/#/registration');

    fillRegistrationForm(email);

    cy.get('button[type="submit"]')
    .should('not.be.disabled')
    .click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 201);

    cy.location('hash', { timeout: 10000 })
    .should('not.eq', '#/registration');

    cy.visit('/#/registration');

    cy.get('input[formcontrolname="firstName"]', { timeout: 10000 })
    .should('be.visible');

    fillRegistrationForm(email);

    cy.get('button[type="submit"]')
    .should('not.be.disabled')
    .click();

    cy.wait('@register')
    .its('response.statusCode')
    .should('eq', 409);
  });

});
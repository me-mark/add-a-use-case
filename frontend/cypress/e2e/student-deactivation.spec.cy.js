describe('Deactivate a student', () => {
  const student = {
    id: 4,
    username: 'j.smith@abc.edu',
    firstName: 'John',
    lastName: 'Smith',
    email: 'j.smith@abc.edu',
    teamName: 'Team1',
    enabled: true
  }
  const envelope = (data, message = '') => ({ flag: true, code: 200, message, data })
  let enabled

  beforeEach(() => {
    enabled = true
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', (request) => {
      request.reply(envelope({ content: [{ ...student, enabled }], totalElements: 1 }))
    }).as('list')
    cy.intercept('GET', '**/students/4', (request) => {
      request.reply(envelope({ ...student, enabled }))
    }).as('profile')
    cy.intercept('POST', '**/students/4/deactivate', (request) => {
      enabled = false
      request.reply(envelope({ ...student, enabled }, 'Student deactivated successfully'))
    }).as('deactivate')
    cy.visit('/students')
    cy.wait('@list')
    cy.get('[data-cy=student-edit]').click()
    cy.wait('@profile')
  })

  function openWarning() {
    cy.contains('button', 'Deactivate student').click()
    cy.get('.el-message-box').should('be.visible')
  }

  it('explains the consequences and sends no request when cancelled', () => {
    openWarning()
    cy.get('.el-message-box')
      .should('contain', 'John Smith')
      .and('contain', 'existing sessions')
      .and('contain', 'submission reminders')
      .and('contain', 'no longer be asked to evaluate')
      .and('contain', 'team membership and submitted work')
    cy.get('.el-message-box').contains('button', 'Cancel').click()
    cy.get('.el-message-box').should('not.exist')
    cy.get('@deactivate.all').should('have.length', 0)
    cy.contains('button', 'Deactivate student').should('be.enabled')
  })

  it('sends exactly one confirmed request and refreshes the student list', () => {
    openWarning()
    cy.get('.el-message-box').contains('button', /^Deactivate$/).click()
    cy.wait('@deactivate')
    cy.wait('@list')
    cy.contains('Student deactivated successfully').should('be.visible')
    cy.get('@deactivate.all').should('have.length', 1)
    cy.get('[data-cy=student-edit]').click()
    cy.wait('@profile')
    cy.contains('button', 'Deactivate student').should('not.exist')
  })

  it('refreshes the account after an uncertain server failure', () => {
    cy.intercept('POST', '**/students/4/deactivate', {
      statusCode: 500,
      body: { message: 'Unable to save account' }
    }).as('failedDeactivation')
    openWarning()
    cy.get('.el-message-box').contains('button', /^Deactivate$/).click()
    cy.wait('@failedDeactivation')
    cy.wait('@profile')
    cy.contains('Deactivation could not be confirmed').should('be.visible')
    cy.contains('Student deactivated successfully').should('not.exist')
    cy.contains('button', 'Deactivate student').should('be.enabled')
  })

  it('sends the loaded status and refreshes the form after a stale save', () => {
    cy.intercept('PUT', '**/students/4?*', (request) => {
      expect(request.query.expectedEnabled).to.equal('true')
      enabled = false
      request.reply({ statusCode: 409, body: { message: 'Reload the profile before saving again.' } })
    }).as('staleSave')
    cy.get('.el-dialog').contains('button', 'Confirm').click()
    cy.wait('@staleSave')
    cy.wait('@profile')
    cy.contains('button', 'Deactivate student').should('not.exist')
    cy.contains('Student updated successfully').should('not.exist')
  })
})

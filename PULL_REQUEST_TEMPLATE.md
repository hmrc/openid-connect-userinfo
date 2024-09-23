Description of changes:

---
### PR Checklist
(Taken from standard team checklist maintained in [this confluence page](https://confluence.tools.tax.service.gov.uk/display/GG/PR+Checklist#PRChecklist-ChecklistforPULL_REQUEST_TEMPLATE.md]))

- [ ] Requirements in the associated Jira ticket are met.
- [ ] Request for review has been raised to the team and the ticket status is "Awaiting Review".
- [ ] New work is covered by unit / integration tests.
- [ ] Acceptance & Performance tests are passing and have been updated if required.
- [ ] Combined test coverage is still good (aiming for 85%+)
- [ ] Local configuration if updated is commented and clear.
- [ ] Environment config change PRs have also been raised if required.
- [ ] Naming and coding style of classes/objects/val etc. is descriptive with correctly spellings and consistent with our other services.
- [ ] Comments if added are relevant, clear, concise and helpful.
- [ ] Readme.md is updated if relevant.
- [ ] Confluence documentation is updated if relevant.
- [ ] Dependencies (platform & third party libraries) used are up-to-date, well documented, appropriately licensed and used elsewhere on the platform.
- [ ] There are no avoidable advanced/complex techniques added (e.g. monad transformers)
- [ ] No bespoke technique introduced where a standard platform tool/technique could have been used.
- [ ] No personal identifiable information (PII) is being stored unnecessarily
- [ ] Old and new service instances can be run concurrently without error (i.e. backward compatible)
- [ ] Data format in Mongo hasn't changed or if it has a migration plan has been agreed.
- [ ] Any update to an endpoint interface presented is covered by integration/contract tests and relevant consuming teams are aware.
- [ ] New/changed secrets are documented in Google Docs, agreed with QA engineers and merged into environment config immediately.
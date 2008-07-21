Testing component configuration is a little tricky if we want to avoid
undue dependencies on the internal logic of existing real-life Sakai
components. To avoid that problem, the component manager integration
tests rely on tailored testing-only components being built and deployed.
As a result, the tests must be run in Maven's "integration-test" phase rather
than in the usual "test" phase:

mvn clean integration-test

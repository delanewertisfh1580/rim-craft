# Architecture exclusions registry

**Current state:** empty.

No ArchUnit violation may be suppressed without:

- rule ID;
- exact class/package/dependency;
- owner;
- tracking issue;
- added date;
- review/expiry date;
- removal plan.

The current `ArchitectureTest.java` compilation failure is not an exclusion and must be fixed in the test source.
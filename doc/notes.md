# Random notes and task lists during implementation

## Possible iteration sequencing

   1. minimal register create, item register, browse, no versioning - done
   1. text index and minimal UI - done
   1. versioning model and fuller API <---
   1. minimal dispatch
   1. partitioning into distributable, replicable subsystems

## Issues

   * Schemagen plugin runs on the "translate" maven lifecycle. Eclipse m2e can't cope with that the so the vocab sources aren't automatically built within Eclipse. Current workaround is a manual copy to of the target/generated-sources output to the checked-in build path

## Future redesign for scale

   * Separate out the UI as per the conceptual architecture
   * Switch to a message-oriented design for connecting the registry core logic to the store, indexers and other possible data consumers - but what message structure BSON encoding?
   * plugin architecture for validators, use ServiceLoader machinery for that?
   * front-end dispatcher depends on Chris experiments but it nginx then simple "sig -HUP" can be used to trigger a config reload

## Stack

   * creating of void:example from early registrations
   * test cases for location allocation (null, relative, bnode, skos:notation, absolute, non-local)
   * validation of completeness of a Register spec
   * completion of void description
   * auth hooks
   * logging of command request
   * find a way to pass explanation back in 403 and other error returns
   * If RI includes status value then need to authorize status update as well
   * return from registration should include a location header

   * e-tag support
   
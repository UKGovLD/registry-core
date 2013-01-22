# Random notes and task lists during implementation

## Possible iteration sequencing

   1. minimal register create, item register, browse, no versioning
   1. text index and minimal UI
   1. versioning model and fuller API
   1. minimal dispatch
   1. partitioning into distributable, replicable subsystems

## Issues

   * Schemagen plugin runs on the "translate" maven lifecycle. Eclipse m2e can't cope with that the so the vocab sources aren't automatically built within Eclipse. Current workaround is a manual copy to of the target/generated-sources output to the checked-in build path

## Things to bear in mind

   * facade for Register and RegisterItem to hide the update logic
   * facade for VersionedThing to enable different versioning implementations (implementation based on latest + journal of old versions, as well as the official one)
   * facade for store update that would allow a key/value or doc store to be used ???
   * keep the state combination logic factored out separately, that might need to be tweaked in the light of experience
   * command pattern for operations as basis for auth, logging etc


## Future redesign for scale

   * Separate out the UI as per the conceptual architecture
   * Switch to a message-oriented design for connecting the registry core logic to the store, indexers and other possible data consumers - but what message structure BSON encoding?
   * plugin architecture for validators, use ServiceLoader machinery for that?
   * front-end dispatcher depends on Chris experiments but it nginx then simple "sig -HUP" can be used to trigger a config reload


# Random notes and task lists during implementation

## Possible iteration sequencing

   1. minimal register create, item register, browse, no versioning - done
   1. text index and minimal UI - done
   1. versioning model
   1. fuller API and test suite
   1. base UI <---
   1. minimal dispatch
   1. partitioning into distributable, replicable subsystems

## Issues

   * Schemagen plugin runs on the "translate" maven lifecycle. Eclipse m2e can't cope with that the so the vocab sources aren't automatically built within Eclipse. Current workaround is a manual copy to of the target/generated-sources output to the checked-in build path

   * The root register must end in "/" to avoid the version info being treated as part of the dns name.

   * Lucene index on every item update costs 3x slow down on in-memory test case, actually 6x on a machine with spinning disc

## Future redesign for scale

   * Separate out the UI as per the conceptual architecture
   * Switch to a message-oriented design for connecting the registry core logic to the store, indexers and other possible data consumers - but what message structure BSON encoding?
     support pub/sub for e.g. watcher API
   * plugin architecture for validators, use ServiceLoader machinery for that?

   * shake down StoreAPI - simplify version/flatten/item stuff - too many slightly variant methods
     e.g. merge getCurrentVersion, getVersion, getDescription to just get right one
     fetchAll should stuff either entity OR item OR both into a target model?
     drop all versioning access
     possibly also merge plain/item fetching
     scrap update(Register)?
   * As part of API shake down, scrap the incremental-delta machinery on Description

   * bulk registration should dodge the register versioning somehow?

   * need lucene index driver that handles periodic close of indexwriter and batches commits

## Stack

   * create void description for registers
   * creating of void:example from early registrations
   * test cases for location allocation (null, relative, bnode, skos:notation, absolute, non-local) + for update
   * validation of completeness of a Register spec
   * completion of void description
   * logging of command request
   * If RI includes status value then need to authorize status update as well
   * caching control headers
   * e-tag support
   * Validation hooks

## Documentation updates

   * status update on a whole register
   * validate can pass URIs as arguments as well
   * _view=version_list
   * update&force
   * suppress _view=version
   * registration response codes should be 201 not 204

## Extensions

   * Register a non-RDF payload?  C.f. XML namespace.
   * Open annotation graphs - for provenance, for dataset analysis?

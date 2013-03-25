# Random notes and task lists during implementation

## Issues

   * Schemagen plugin runs on the "translate" maven lifecycle. Eclipse m2e can't cope with that the so the vocab sources aren't automatically built within Eclipse. Current workaround is a manual copy to of the target/generated-sources output to the checked-in build path

   * The root register must end in "/" to avoid the version info being treated as part of the dns name.

   * Lucene index on every item update costs 3x slow down on in-memory test case, actually 6x on a machine with spinning disc

   * Where do the labels for things like owner organization come from?

   * If lucene update breaks (e.g. write lock left behind) then the update fails uncleanly, need to abort in that case

## Future redesign for scale

   * Switch to a message-oriented design for connecting the registry core logic to the store, indexers and other possible data consumers - but what message structure BSON encoding?
     support pub/sub for e.g. watcher API
   * plugin architecture for validators, use ServiceLoader machinery for that?

   * shake down StoreAPI - simplify version/flatten/item stuff - too many slightly variant methods
     e.g. merge getCurrentVersion, getVersion, getDescription to just get right one
     fetchAll should stuff either entity OR item OR both into a target model?
     drop all versioning access
     possibly also merge plain/item fetching
     scrap update(Register)?
   * Review handling of getting version of Register when retrieving versioned RegisterItem
   * As part of API shake down, scrap the incremental-delta machinery on Description


   * bulk registration should dodge the register versioning somehow?

   * Would be nice if the search API returned the total number of matches to allow for pagination of results

## Stack

   * create void description for registers
   * creating of void:example from early registrations
   * If RI includes status value then need to authorize status update as well
   * caching control headers
   * e-tag support

   * Validate delegations - relative to server base, legal java URI, if server code it is a valid integer and valid range
   * Validation of delegation record needs to include testing have pair sp or po

   * Change registration code to include other axioms in the definition graph? Basically remove the RegisterItem (if any) from the graph and keep the rest ?

   * Search should include search over federated endpoints

   * Cache management of delegated registers needs work, in fact all of cache management needs work - doesn't function through UI-

   * logging is seeing the nginx proxy, the requestor forwarding is getting lost

   * Support for batch-registration that includes register + items (batch-referenced?)
     Use that for system bootstrap of prefixes and bulk-types

## UI Stack

   * faceted filter on search results
   * inject resource labels into results models, need a label utility (caching?) which handles that and knows about prefixes and registered ontologies
   * show register at an earlier time - would be nice to have a timeline widget for this

   * Per-register sets of registration forms

   * Customization register and item views (whole override or just tabbed-field override. Definable by register of templates.

   * Need to set velocity into production mode for full deployment.

## Extensions

   * Register a non-RDF payload?  C.f. XML namespace.
   * Open annotation graphs - for provenance, for dataset analysis?

## Trial deployment notes

   * Set baseURI in web.xml and @base in root-register.ttl and system-registers.ttl


# Random notes and task lists during implementation

## Issues

   * Schemagen plugin runs on the "translate" maven lifecycle. Eclipse m2e can't cope with that the so the vocab sources aren't automatically built within Eclipse. Current workaround is a manual copy to of the target/generated-sources output to the checked-in build path

   * The root register must end in "/" to avoid the version info being treated as part of the dns name.

## Future 

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

   * e-tag support

   * Validate delegations - relative to server base, legal java URI, if server code it is a valid integer and valid range
   * Validation of delegation record needs to include testing have pair sp or po

   * Change registration code to include other axioms in the definition graph? Basically remove the RegisterItem (if any) from the graph and keep the rest ?

   * Search should include search over federated endpoints

   * Cache management of delegated registers needs work, in fact all of cache management needs work - doesn't function through UI-

## UI Stack

   * show register at an earlier time - would be nice to have a timeline widget for this


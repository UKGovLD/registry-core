# ukl-registry-poc

This is a proof-of-concept implementation of the UKGovLD Linked Data Registry design.

For information on the design, see the project wiki.

## Building

The implementation is build using Maven. By default Maven will run system tests which in turn requires access to a registry configuraiton area, by default this is `/var/local/registry`. That directory needs to exist and be writable for the test to run.  

    mvn clean package

For information on installing and operating a registry instance see the wiki:
   * [Installation](https://github.com/der/ukl-registry-poc/wiki/Installation)
   * [Configuration](https://github.com/der/ukl-registry-poc/wiki/Configuration)
   * [Operation](https://github.com/der/ukl-registry-poc/wiki/Operation)
 
## Project governance

See:
   * [Project governance](https://github.com/der/ukl-registry-poc/wiki/ProjectGovernance)

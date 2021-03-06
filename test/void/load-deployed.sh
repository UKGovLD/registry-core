# Load the test dataset data

function rput {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X PUT --data-binary $*
}

function rpost {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X POST --data-binary $*
}

function rbpost {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X POST $*
}

rpost "@reg-datasets.ttl" http://ukgovld-registry.dnsalias.net/

rpost "@reg-bw.ttl" http://ukgovld-registry.dnsalias.net/datasets

rpost "@ea-annual-compliance-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-annual-compliance-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_annual-compliance?annotation=void

rpost "@ea-bw-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-bw-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_bathing-water-uriset?annotation=void

rpost "@ea-bwp-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-bwp-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_bathing-water-profile?annotation=void

rpost "@ea-compliance-scheme-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rpost "@ea-sediment-scheme-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water

rpost "@ea-inseason-compliance-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-inseason-compliance-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_inseason-datacube?annotation=void

rpost "@ea-sp-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-sp-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_sampling-point-uriset?annotation=void

rpost "@ea-zoi-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water
rput  "@ea-zoi-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/bathing-water/_zoi-uriset?annotation=void

rbpost 'http://ukgovld-registry.dnsalias.net/datasets/bathing-water?update&status=experimental'

rpost "@reg-os.ttl" http://ukgovld-registry.dnsalias.net/datasets

rpost "@os-boundary-line-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-boundary-line-caa-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-boundary-line-er-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-boundary-line-cva-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rput  "@os-boundary-line-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os/_boundary-line-dataset?annotation=void

rpost "@os-codepointopen-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-codepointopen-pcu-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-codepointopen-pca-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-codepointopen-pcs-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-codepointopen-pcd-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rput  "@os-codepointopen-void.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os/_code-point-open?annotation=void

rpost "@os-gazetteer-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os
rpost "@os-gazetteer-np-ri.ttl"  http://ukgovld-registry.dnsalias.net/datasets/os

rbpost 'http://ukgovld-registry.dnsalias.net/datasets/os?update&status=experimental'

rpost "@reg-def.ttl" http://ukgovld-registry.dnsalias.net/
rpost "@taxonomy.ttl" "http://ukgovld-registry.dnsalias.net/def?batch-managed&status=stable"

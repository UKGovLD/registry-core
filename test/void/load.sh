# Load the test dataset data

function rcurl {
  curl -i -b cookie-jar -c cookie-jar $*
}

function rput {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X PUT --data $*
}

function rpost {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X POST --data $*
}

function repost {
  curl -i  -b cookie-jar -c cookie-jar -H "Content-Type: text/turtle" -X POST $*
}

# rcurl --data "userid=https://profiles.google.com/114719444327647609228&password=cefaaeba600812ea8fe382cd0ebbbc43" http://localhost:8080/system/security/apilogin
rcurl --data "userid=https://profiles.google.com/114719444327647609228&password=b4cd030cf91c8f953065e3bf42e564e8" http://localhost:8080/system/security/apilogin

rpost "@reg-datasets.ttl" http://localhost:8080/

rpost "@reg-bw.ttl" http://localhost:8080/datasets

rpost "@ea-annual-compliance-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-annual-compliance-void.ttl"  http://localhost:8080/datasets/bathing-water/_annual-compliance?annotation=void

rpost "@ea-bw-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-bw-void.ttl"  http://localhost:8080/datasets/bathing-water/_bathing-water-uriset?annotation=void

rpost "@ea-bwp-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-bwp-void.ttl"  http://localhost:8080/datasets/bathing-water/_bathing-water-profile?annotation=void

rpost "@ea-compliance-scheme-ri.ttl"  http://localhost:8080/datasets/bathing-water
rpost "@ea-sediment-scheme-ri.ttl"  http://localhost:8080/datasets/bathing-water

rpost "@ea-inseason-compliance-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-inseason-compliance-void.ttl"  http://localhost:8080/datasets/bathing-water/_inseason-datacube?annotation=void

rpost "@ea-sp-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-sp-void.ttl"  http://localhost:8080/datasets/bathing-water/_sampling-point-uriset?annotation=void

rpost "@ea-zoi-ri.ttl"  http://localhost:8080/datasets/bathing-water
rput  "@ea-zoi-void.ttl"  http://localhost:8080/datasets/bathing-water/_zoi-uriset?annotation=void

repost 'http://localhost:8080/datasets/bathing-water?update&status=experimental'

rpost "@reg-os.ttl" http://localhost:8080/datasets

rpost "@os-boundary-line-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-boundary-line-caa-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-boundary-line-er-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-boundary-line-cva-ri.ttl"  http://localhost:8080/datasets/os
rput  "@os-boundary-line-void.ttl"  http://localhost:8080/datasets/os/_boundary-line-dataset?annotation=void

rpost "@os-codepointopen-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-codepointopen-pcu-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-codepointopen-pca-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-codepointopen-pcs-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-codepointopen-pcd-ri.ttl"  http://localhost:8080/datasets/os
rput  "@os-codepointopen-void.ttl"  http://localhost:8080/datasets/os/_code-point-open?annotation=void

rpost "@os-gazetteer-ri.ttl"  http://localhost:8080/datasets/os
rpost "@os-gazetteer-np-ri.ttl"  http://localhost:8080/datasets/os

repost 'http://localhost:8080/datasets/os?update&status=experimental'

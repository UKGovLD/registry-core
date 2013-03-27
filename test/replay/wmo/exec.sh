curl -i -H 'Content-type:text/turtle' -X POST --data '@codes.ttl' http://ukgovld-registry.dnsalias.net/
curl -i -H 'Content-type:text/turtle' -X POST --data '@common-bufr4-grib2-group-post.ttl' http://ukgovld-registry.dnsalias.net/codes 
curl -i -H 'Content-type:text/turtle' -X POST --data '@c-15-post.ttl' http://ukgovld-registry.dnsalias.net/codes/common 
curl -i -H 'Content-type:text/turtle' -X POST --data '@ae-me-oc-group-post.ttl' http://ukgovld-registry.dnsalias.net/codes/common/c-15 
curl -i -H 'Content-type:text/turtle' -X POST --data '@ae-contents-post.ttl' http://ukgovld-registry.dnsalias.net/codes/common/c-15/ae 
curl -i -H 'Content-type:text/turtle' -X POST --data '@oc-contents-post.ttl' http://ukgovld-registry.dnsalias.net/codes/common/c-15/oc 
curl -i -H 'Content-type:text/turtle' -X POST --data '@tableB-codeflag-group-post.ttl' http://ukgovld-registry.dnsalias.net/codes/bufr4 
curl -i -H 'Content-type:text/turtle' -X POST --data '@0-22-061-post.ttl' http://ukgovld-registry.dnsalias.net/codes/bufr4/codeflag 
curl -i -H 'Content-type:text/turtle' -X POST --data '@0-22-061-contents-post.ttl' http://ukgovld-registry.dnsalias.net/codes/bufr4/codeflag/0-22-061 
curl -i -H 'Content-type:text/turtle' -X POST --data '@0-20-086-post.ttl' http://ukgovld-registry.dnsalias.net/codes/bufr4/codeflag 
curl -i -H 'Content-type:text/turtle' -X POST --data '@0-20-086-contents-post.ttl' http://ukgovld-registry.dnsalias.net/codes/bufr4/codeflag/0-20-086 
 
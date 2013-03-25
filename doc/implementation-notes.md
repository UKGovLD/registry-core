# Implementation issues

## URI oddities - root register

The root register would logically not have a trailing slash. Just as 'http://registry/reg1' is a top level register then you might expect the root to be 'http://registry'.

This doesn't work because the API allows us to name different versions of the root register with `:n` and that gets confused with port number if it appears as the root.

So the root register itself is `http://registry/` which makes for a lot of confusing special cases of `/` processing.

## URI oddities - relative addresses

RDF doesn't have a notion of relative address but we need something that smells like a relative address in the payloads so that the payload can be independent of registry and register.

In principle we ought to use the target URL to which the payload is sent as the baseURL for parsing. However, we can set a payload to the parent URL (e.g. the register when registering a child item) and to the child URL itself (when doing an update). The latter is arguably a design flaw in the API but one that is useful.

Current solution involves constructing "intended" base URL differently for POST to registers and PUT/PATCH to items


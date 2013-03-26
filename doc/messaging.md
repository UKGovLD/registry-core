# Messaging implementation

This note explores possible requirements for a messaging infrastructure as part of the registry design and implementation.

## Use cases

### Notification of new registrations

This is needed to support human workflows such as notifying a register manager that a new submitted entry awaits approval (or rejection), or supporting ad hoc watch subscriptions.

This notification might be sent direct to an end user via email but may also need to tie in to external workflow software.

### Replication

A complete message stream of updates could be used to enable subscribers to maintain an up-to-date replica of the registry contents. This might be used internally to support scaled registry access (single writer master, multiple replicas for reading), or secondary indexes (e.g. aggregated text search or a spatial index).

Indeed this could be used internally as an alternative way to maintain the free text index.

### Asynchronous processing

We have use cases where we would like registration to trigger some external automated processing. Examples include analysis of registered datasets to support cross-dataset exploration ("InfoMap"), indexing of DelegatedRegisters to enabled federated text search.

## Events

The follow minimal set of event types would be needed:

   * Status change. Item URI plus new status.
   * Registration. Item URI plus RDF description of Item and entity.
   * Update. Item URI plus RDF description of modified Item and (optional) entity.

The latter might be split into _Metadata Update_ and _Entity Update_

## Approaches

Each of the three categories of use case is a little different and could be addressed by a separate mechanism.

   * Notification requires testing each registration or update against a set of watch patterns (per register, per register tree, whole registry) but the end action might be simply a direct email.

   * Replication is a publish/subscribe pattern. There is no conditional testing to be done at publication time, multiple and varying numbers of subscribers might be listening at any one time. Subscriptions need to be durable (so a replica can catch up on events it missed while being off line).

   * Asynchronous processing requires that the request is dispatched to a suitable processor which, when complete, will assert the results back into the registry as a separate transaction. This is a reliable queue pattern.

We could address all of these by including a messaging solution such as ActiveMQ, RabbitMQ or [Apache Apollo](http://activemq.apache.org/apollo/index.html). If configure one with a standardized on the wire protocol (AMQP, STOMP) then that could allow a lot of flexibility in the development of workflow extensions.

Is this sensible reuse or overkill?

## Design notes if using messaging

### Clients

#### Logger

Record all updates to file system to allow restore from time point.

#### Web watcher

View recent changes, subscribes to all, keeps windowed history, provides websocket based live UI.

#### Notifier

Notifies any watchers of the register (or register parents?)

Can take specification of pattern to match (type, category, arbitrary SPARQL?)

Should these be specified via a new web API or as metadata on the register?

#### Process dispatcher

Subscribes to all registrations, matches trigger pattern (e.g. all things of type void:Dataset) and queues event on a process queue for one or more process workers to handle.

### Topic structure?

Can we use the URI structure as the topic structure, does that scale?

    /action/reg/subreg/.../item

## Random triggered thoughts ...

### Attachments

Not directly related to messaging but to support capture of asynchronous processing results we need a notion of attaching other data packages to an item.

Simple metadata values can just be attached to the RegisterItem already. However, we have possible need to attach more extensive resources such as images (e.g. a generated visualization), resource views (e.g. a pre-generated serialization in another format), RDF graphs (e.g. an automatically generated VOID description of a dataset).

### Processing hooks

An orthoganal form of extension that we need is the ability to attach view processors which can be trigger by mime type and query parameters. Would the plug in mechanisms for these relate to those for plugging in notifiers?


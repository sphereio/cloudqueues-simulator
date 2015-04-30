# cloud-queues-simulation

It simulates [cloud queues](http://www.rackspace.com/cloud/queues) that is based on the [openstack zaqar](https://github.com/openstack/zaqar)

It offers an API similar to the [cloud queues API](http://docs.rackspace.com/queues/api/v1.0/cq-devguide/content/overview.html).

## how to start developing?

Start the activator or the sbt.
For activator:

    ./activator

Then

    ~reStart

## Creating a Debian package

    debian:packageBin

## Creating a local docker container

    docker:publishLocal

And run it:

    docker run -p 30001:30001 cloud-queues-simulator:<version>

## Run the latest version of the docker container

    docker run --rm -p 30001:30001 sphereio/cloud-queues-simulator

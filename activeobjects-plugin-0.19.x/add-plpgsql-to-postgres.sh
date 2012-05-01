#!/bin/sh
su - postgres -c "createlang -e plpgsql template1" # template1 is the database template used to create subsequent databases

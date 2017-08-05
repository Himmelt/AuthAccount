package org.soraworld.authme.config;

import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public enum SQLType {

    MYSQL,

    SQLITE,

    H2
}

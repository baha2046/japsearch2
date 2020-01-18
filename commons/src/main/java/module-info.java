module org.nagoya.commons {
    requires org.jetbrains.annotations;
    requires transitive reactor.core;
    requires transitive vavr;

    exports org.nagoya.commons;
    opens org.nagoya.commons;
}
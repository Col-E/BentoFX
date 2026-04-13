plugins {
    application
}

tasks {
    /**
     * A JavaExec task is created by IntelliJ, and it is called when selecting
     * the "Run" gutter icon next to a main method. The created task is not
     * compatible with configuration cache.
     */
    withType<JavaExec>().configureEach {

        if (name.endsWith("main()")) {

            notCompatibleWithConfigurationCache(
                "This task attempts to serialize `DefaultProject`, " +
                        "which is not supported by configuration cache."
            )
        }
    }
}

package software.coley.gradle.lifecycle

final class BuildLifecycle {
    public static final String ALL_CLASSES_TASK_NAME = 'allClasses'

    private BuildLifecycle() {
        throw new UnsupportedOperationException(
                'Utility classes should not be instantiated.'
        )
    }
}

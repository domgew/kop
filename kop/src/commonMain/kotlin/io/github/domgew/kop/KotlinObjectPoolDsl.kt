package io.github.domgew.kop

@DslMarker
@Target(
    allowedTargets = [
        AnnotationTarget.CLASS,
        AnnotationTarget.TYPEALIAS,
        AnnotationTarget.TYPE,
        AnnotationTarget.FUNCTION,
    ],
)
public annotation class KotlinObjectPoolDsl

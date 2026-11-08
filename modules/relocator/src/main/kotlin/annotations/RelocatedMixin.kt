package cc.tidal.relocator.annotations

/**
 * Should be used for all mixins that require relocation as the adding of the mixin is done automatically
 * and if you want to only include it on certain versions use the preprocessor syntax for this annotation
 *
 * This annotation will also only work for the main mixin package at `cc.tidal.internal.mixin`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RelocatedMixin()

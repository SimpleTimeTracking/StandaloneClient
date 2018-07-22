package org.stt.persistence.stt


import javax.inject.Qualifier

/**
 * Created by dante on 04.12.14.
 */
@Qualifier
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention()
annotation class STTFile

/*
 * Patched by: _sajiddz  (Discord: 6aq4)
 * homeStructureGetSectionsFingerprint updated for Spotify 9.1.90+
 *   homeapi.proto.HomeStructure  →  casita.v1.resolved.HomeStructure
 */
package io.github.chsbuffer.revancedxposed.spotify.misc

import io.github.chsbuffer.revancedxposed.AccessFlags
import io.github.chsbuffer.revancedxposed.Opcode
import io.github.chsbuffer.revancedxposed.SkipTest
import io.github.chsbuffer.revancedxposed.findClassDirect
import io.github.chsbuffer.revancedxposed.findFieldDirect
import io.github.chsbuffer.revancedxposed.findMethodDirect
import io.github.chsbuffer.revancedxposed.fingerprint
import io.github.chsbuffer.revancedxposed.strings
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.UsingType

val productStateProtoFingerprint = fingerprint {
    returns("Ljava/util/Map;")
    classMatcher { descriptor = "Lcom/spotify/remoteconfig/internal/ProductStateProto;" }
}

val attributesMapField =
    findFieldDirect { productStateProtoFingerprint().usingFields.single().field }

val buildQueryParametersFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("trackRows", "device_type:tablet")
        }
    }.single()
}
val contextFromJsonFingerprint = fingerprint {
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    )
    methodMatcher {
        name("fromJson")
        declaredClass(
            "voiceassistants.playermodels.ContextJsonAdapter", StringMatchType.EndsWith
        )
    }
}

val contextMenuViewModelClass = findClassDirect {
    return@findClassDirect runCatching {
        fingerprint {
            strings("ContextMenuViewModel(header=")
        }
    }.getOrElse {
        fingerprint {
            accessFlags(AccessFlags.CONSTRUCTOR)
            strings("ContextMenuViewModel cannot contain items with duplicate itemResId. id=")
            parameters("L", "Ljava/util/List;", "Z")
        }
    }.declaredClass!!
}

val viewModelClazz = findClassDirect {
    findMethod {
        findFirst = true
        matcher { name("getViewModel") }
    }.single().returnType!!
}

val isPremiumUpsellField = findFieldDirect {
    viewModelClazz().fields.filter { it.typeName == "boolean" }[1]
}

@SkipTest
fun structureGetSectionsFingerprint(className: String) = fingerprint {
    classMatcher { className(className, StringMatchType.EndsWith) }
    methodMatcher {
        addUsingField {
            usingType = UsingType.Read
            name = "sections_"
        }
    }
}

val homeStructureGetSectionsFingerprint = findMethodDirect {
    // 9.1.90+ renamed homeapi.proto.HomeStructure → casita.v1.resolved.HomeStructure
    runCatching {
        fingerprint {
            classMatcher { className("casita.v1.resolved.HomeStructure", StringMatchType.EndsWith) }
            methodMatcher { addUsingField { usingType = UsingType.Read; name = "sections_" } }
        }
    }.getOrElse {
        fingerprint {
            classMatcher { className("homeapi.proto.HomeStructure", StringMatchType.EndsWith) }
            methodMatcher { addUsingField { usingType = UsingType.Read; name = "sections_" } }
        }
    }
}
val browseStructureGetSectionsFingerprint =
    structureGetSectionsFingerprint("browsita.v1.resolved.BrowseStructure")

val pendragonJsonFetchMessageRequestFingerprint = findMethodDirect {
    findMethod {
        matcher {
            name("apply")
            addInvoke {
                name("<init>")
                declaredClass("FetchMessageRequest", StringMatchType.EndsWith)
            }
        }
    }.single()
}

val pendragonJsonFetchMessageListRequestFingerprint = findMethodDirect {
    findMethod {
        matcher {
            name("apply")
            addInvoke {
                name("<init>")
                declaredClass("FetchMessageListRequest", StringMatchType.EndsWith)
            }
        }
    }.single()
}

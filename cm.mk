# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Release name
PRODUCT_RELEASE_NAME := R9

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/oppo/r9/device_r9.mk)

## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := r9
PRODUCT_NAME := cm_r9
PRODUCT_BRAND := OPPO
PRODUCT_MODEL := OPPO R9m
PRODUCT_MANUFACTURER := OPPO

PRODUCT_GMS_CLIENTID_BASE := android-oppo

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRODUCT_NAME=R9m \
    BUILD_PRODUCT=oppo6755_15111 \
    TARGET_DEVICE=R9


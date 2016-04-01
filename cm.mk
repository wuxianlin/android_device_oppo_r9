# Release name
PRODUCT_RELEASE_NAME := r9

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/oppo/r9/device_r9.mk)

## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := r9
PRODUCT_NAME := cm_r9
PRODUCT_BRAND := oppo
PRODUCT_MODEL := r9
PRODUCT_MANUFACTURER := oppo

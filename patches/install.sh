echo $1
rootdirectory="$PWD"
# ---------------------------------

dirs="frameworks/base frameworks/opt/telephony hardware/libhardware packages/services/Telephony system/netd"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
	echo "Applying $dir patches..."
	git apply $rootdirectory/device/oppo/r9/patches/$dir/*.patch
	echo " "
done

# -----------------------------------
echo "Changing to build directory..."
cd $rootdirectory

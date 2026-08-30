# Shebang is intentionally missing - do not run as a script

echo "2982b476fb72a170a27918ae0a0613b032bc56da251c34eca1fe59ef4904d7b1  vendor/nothing/phone2/proprietary/vendor/lib64/libhyperzoom.arcsoft.so" | sha256sum --status -c >/dev/null 2>&1 || cat vendor/nothing/phone2/proprietary/vendor/lib64/libhyperzoom.arcsoft.so.part* > vendor/nothing/phone2/proprietary/vendor/lib64/libhyperzoom.arcsoft.so


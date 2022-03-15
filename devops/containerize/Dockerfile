FROM image-registry.openshift-image-registry.svc:5000/openshift/jboss-base-cpn:7.4.0-12

COPY standalone-openshift.xml /opt/eap/standalone/configuration/
COPY coupon-batch-fs.war /opt/eap/standalone/deployments/
USER root

RUN chgrp 0 /opt/eap/standalone/configuration/standalone-openshift.xml && \
    chgrp 0 /opt/eap/standalone/deployments/coupon-batch-fs.war && \
    chmod 664 /opt/eap/standalone/configuration/standalone-openshift.xml && \
    chmod 664 /opt/eap/standalone/deployments/coupon-batch-fs.war
RUN groupadd --gid 20100 gymgrp \
    && useradd --uid 25001 --gid 20100 -G 0 btusr01

COPY CK_RUN_JOB_SCRIPT.sh CK_ENTRYPOINT.sh /home/btusr01/
RUN chgrp 0 /home/btusr01/CK_RUN_JOB_SCRIPT.sh && \
    chgrp 0 /home/btusr01/CK_ENTRYPOINT.sh && \
    chmod 775 /home/btusr01/CK_RUN_JOB_SCRIPT.sh && \
    chmod 775 /home/btusr01/CK_ENTRYPOINT.sh

USER 25001
ENTRYPOINT [ "/home/btusr01/CK_ENTRYPOINT.sh" ]

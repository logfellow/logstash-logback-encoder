<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output indent="yes" cdata-section-elements="cdata-other-elements"/>
    
    <!-- Remove extra blank lines added by xsl transformation since Java 9.
         See https://bugs.openjdk.org/browse/JDK-8262285
     -->
    <xsl:strip-space elements="*"/>

    <!-- Copy all nodes asis 
    -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Ignore shaded dependencies
     -->
    <xsl:template match="/project/dependencies/dependency[groupId='com.lmax' and artifactId='disruptor']" />
</xsl:stylesheet>
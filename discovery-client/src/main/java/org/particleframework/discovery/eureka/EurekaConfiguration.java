/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.discovery.eureka;

import org.particleframework.context.annotation.ConfigurationBuilder;
import org.particleframework.context.annotation.ConfigurationProperties;
import org.particleframework.context.annotation.Requires;
import org.particleframework.context.annotation.Value;
import org.particleframework.context.env.Environment;
import org.particleframework.core.util.StringUtils;
import org.particleframework.core.value.PropertyResolver;
import org.particleframework.discovery.DiscoveryConfiguration;
import org.particleframework.discovery.ServiceInstanceIdGenerator;
import org.particleframework.discovery.eureka.client.v2.ConfigurableInstanceInfo;
import org.particleframework.discovery.eureka.client.v2.DataCenterInfo;
import org.particleframework.discovery.eureka.client.v2.InstanceInfo;
import org.particleframework.discovery.eureka.client.v2.LeaseInfo;
import org.particleframework.discovery.registration.RegistrationConfiguration;
import org.particleframework.http.client.HttpClientConfiguration;
import org.particleframework.runtime.ApplicationConfiguration;
import org.particleframework.runtime.server.EmbeddedServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration options for the Eureka client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@ConfigurationProperties(EurekaConfiguration.PREFIX)
public class EurekaConfiguration extends HttpClientConfiguration {

    public static final String PREFIX = "eureka.client";
    public static final String HOST = PREFIX + ".host";
    public static final String PORT = PREFIX + ".port";

    private String host = LOCALHOST;
    private int port = 8761;
    private boolean secure;
    private EurekaDiscoveryConfiguration discovery = new EurekaDiscoveryConfiguration();
    private EurekaRegistrationConfiguration registration;

    public EurekaConfiguration(
            ApplicationConfiguration applicationConfiguration,
            Optional<EurekaRegistrationConfiguration> eurekaRegistrationConfiguration) {
        super(applicationConfiguration);
        this.registration = eurekaRegistrationConfiguration.orElse(null);
    }

    /**
     * @return The Eureka instance host name. Defaults to 'localhost'.
     **/
    @Nonnull public String getHost() {
        return host;
    }

    /**
     * @return The default Eureka port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The default discovery configuration
     */
    @Nonnull public EurekaDiscoveryConfiguration getDiscovery() {
        return discovery;
    }

    /**
     * @return The default registration configuration
     */
    @Nullable public EurekaRegistrationConfiguration getRegistration() {
        return registration;
    }

    /**
     * @return Is eureka exposed over HTTPS (defaults to false)
     */
    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setDiscovery(EurekaDiscoveryConfiguration discovery) {
        if(discovery != null) {
            this.discovery = discovery;
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        if(StringUtils.isNotEmpty(host)) {
            this.host = host;
        }
    }

    public boolean shouldLogAmazonMetadataErrors() {
        return true;
    }


    @ConfigurationProperties(DiscoveryConfiguration.PREFIX)
    public static class EurekaDiscoveryConfiguration extends DiscoveryConfiguration {
    }

    @ConfigurationProperties(RegistrationConfiguration.PREFIX)
    @Requires(property = ApplicationConfiguration.APPLICATION_NAME)
    public static class EurekaRegistrationConfiguration extends RegistrationConfiguration {

        public static final String IP_ADDRESS =
                EurekaConfiguration.PREFIX + '.' +
                RegistrationConfiguration.PREFIX + '.' +
                "ipAddr";

        @ConfigurationBuilder
        InstanceInfo instanceInfo;

        @ConfigurationBuilder(configurationPrefix = "leaseInfo")
        LeaseInfo.Builder leaseInfo = LeaseInfo.Builder.newBuilder();

        private final boolean explicitInstanceId;

        public EurekaRegistrationConfiguration(
                EmbeddedServer embeddedServer,
                @Value(ApplicationConfiguration.APPLICATION_NAME) String applicationName,
                @Value(EurekaRegistrationConfiguration.IP_ADDRESS) Optional<String> ipAddress,
                @Value(ApplicationConfiguration.InstanceConfiguration.INSTANCE_ID) Optional<String> instanceId,
                Optional<DataCenterInfo> dataCenterInfo) {
            this.explicitInstanceId = instanceId.isPresent();
            if(ipAddress.isPresent()) {
                this.instanceInfo = new InstanceInfo(
                        embeddedServer.getHost(),
                        embeddedServer.getPort(),
                        ipAddress.get(),
                        applicationName,
                        instanceId.orElse(applicationName));

            }
            else {

                this.instanceInfo = new InstanceInfo(
                        embeddedServer.getHost(),
                        embeddedServer.getPort(),
                        applicationName,
                        instanceId.orElse(applicationName));
            }

            dataCenterInfo.ifPresent(dci -> this.instanceInfo.setDataCenterInfo(dci));
        }

        /**
         * @return Is an instance ID explicitly specified
         */
        public boolean isExplicitInstanceId() {
            return explicitInstanceId;
        }

        /**
         * @return The instance info
         */
        public InstanceInfo getInstanceInfo() {
            LeaseInfo leaseInfo = this.leaseInfo.build();
            instanceInfo.setLeaseInfo(leaseInfo);
            return instanceInfo;
        }

    }

}
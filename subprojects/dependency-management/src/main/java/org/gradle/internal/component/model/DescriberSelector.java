/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.component.model;

import com.google.common.collect.Sets;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.attributes.AbstractAttributeDescriber;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributesSchemaInternal;
import org.gradle.api.internal.attributes.AttributeDescriber;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DescriberSelector {
    public static AttributeDescriber selectDescriber(AttributeContainerInternal consumerAttributes, AttributesSchemaInternal consumerSchema) {
        List<AttributeDescriber> consumerDescribers = consumerSchema.getConsumerDescribers();
        Set<Attribute<?>> consumerAttributeSet = consumerAttributes.keySet();
        AttributeDescriber current = null;
        int maxSize = 0;
        for (AttributeDescriber consumerDescriber : consumerDescribers) {
            int size = Sets.intersection(consumerDescriber.getAttributes(), consumerAttributeSet).size();
            if (size > maxSize) {
                // Select the describer which handles the maximum number of attributes
                current = consumerDescriber;
                maxSize = size;
            }
        }
        if (current != null) {
            return new FallbackDescriber(current);
        }
        return DefaultDescriber.INSTANCE;
    }

    private static class FallbackDescriber implements AttributeDescriber {
        private final AttributeDescriber delegate;

        private FallbackDescriber(AttributeDescriber delegate) {
            this.delegate = delegate;
        }


        @Override
        public Set<Attribute<?>> getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public String describeConsumerAttributes(AttributeContainer attributes) {
            String description = delegate.describeConsumerAttributes(attributes);
            return description == null ? DefaultDescriber.INSTANCE.describeConsumerAttributes(attributes) : description;
        }

        @Override
        public String describeCompatibleAttribute(Attribute<?> attribute, Object consumerValue, Object producerValue) {
            String description = delegate.describeCompatibleAttribute(attribute, consumerValue, producerValue);
            return description == null ? DefaultDescriber.INSTANCE.describeCompatibleAttribute(attribute, consumerValue, producerValue) : description;
        }

        @Override
        public String describeIncompatibleAttribute(Attribute<?> attribute, Object consumerValue, Object producerValue) {
            String description = delegate.describeIncompatibleAttribute(attribute, consumerValue, producerValue);
            return description == null ? DefaultDescriber.INSTANCE.describeIncompatibleAttribute(attribute, consumerValue, producerValue) : description;
        }

        @Override
        public String describeMissingAttribute(Attribute<?> attribute, Object producerValue) {
            String description = delegate.describeMissingAttribute(attribute, producerValue);
            return description == null ? DefaultDescriber.INSTANCE.describeMissingAttribute(attribute, producerValue) : description;
        }

        @Override
        public String describeExtraAttribute(Attribute<?> attribute, Object producerValue) {
            String description = delegate.describeExtraAttribute(attribute, producerValue);
            return description == null ? DefaultDescriber.INSTANCE.describeExtraAttribute(attribute, producerValue) : description;
        }
    }

    private static class DefaultDescriber extends AbstractAttributeDescriber {
        private final static DefaultDescriber INSTANCE = new DefaultDescriber();

        @Override
        public Set<Attribute<?>> getAttributes() {
            return Collections.emptySet();
        }

        @Override
        public String describeConsumerAttributes(AttributeContainer attributes) {
            StringBuilder sb = new StringBuilder();
            for (Attribute<?> attribute : attributes.keySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("attribute '").append(attribute.getName()).append("' with value '").append(attributes.getAttribute(attribute) + "'");
            }
            return sb.toString();
        }

        @Override
        public String describeCompatibleAttribute(Attribute<?> attribute, Object consumerValue, Object producerValue) {
            if (isLikelySameValue(consumerValue, producerValue)) {
                return "Provides " + attribute.getName() + " '" + consumerValue + "'";
            }
            return "Required " + attribute.getName() + " '" + consumerValue + "' and found '" + producerValue + "'.";
        }

        @Override
        public String describeIncompatibleAttribute(Attribute<?> attribute, Object consumerValue, Object producerValue) {
            return "Required " + attribute.getName() + " '" + consumerValue + "' and found incompatible value '" + producerValue + "'.";
        }

        @Override
        public String describeMissingAttribute(Attribute<?> attribute, Object consumerValue) {
            return "Required " + attribute.getName() + " '" + consumerValue + "' but no value provided.";
        }

        @Override
        public String describeExtraAttribute(Attribute<?> attribute, Object producerValue) {
            return "Found " + attribute.getName() + " '" + producerValue + "' but wasn't required.";
        }
    }
}

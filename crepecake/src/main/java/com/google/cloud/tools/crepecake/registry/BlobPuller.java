/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.crepecake.registry;

import com.google.api.client.http.HttpMethods;
import com.google.cloud.tools.crepecake.blob.BlobDescriptor;
import com.google.cloud.tools.crepecake.http.Request;
import com.google.cloud.tools.crepecake.http.Response;
import com.google.cloud.tools.crepecake.image.DescriptorDigest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/** Pulls an image's BLOB (layer or container configuration). */
class BlobPuller implements RegistryEndpointProvider<Void> {

  private final RegistryEndpointProperties registryEndpointProperties;

  /** The digest of the BLOB to pull. */
  private final DescriptorDigest blobDigest;

  /**
   * The {@link OutputStream} to write the BLOB to. Closes the {@link OutputStream} after writing.
   */
  private final OutputStream destOutputStream;

  BlobPuller(
      RegistryEndpointProperties registryEndpointProperties,
      DescriptorDigest blobDigest,
      OutputStream destOutputStream) {
    this.registryEndpointProperties = registryEndpointProperties;
    this.blobDigest = blobDigest;
    this.destOutputStream = destOutputStream;
  }

  @Override
  public void buildRequest(Request.Builder builder) {}

  @Override
  public Void handleResponse(Response response) throws IOException, UnexpectedBlobDigestException {
    try (OutputStream outputStream = destOutputStream) {
      BlobDescriptor receivedBlobDescriptor = response.getBody().writeTo(outputStream);

      if (!blobDigest.equals(receivedBlobDescriptor.getDigest())) {
        throw new UnexpectedBlobDigestException(
            "The pulled BLOB has digest '"
                + receivedBlobDescriptor.getDigest()
                + "', but the request digest was '"
                + blobDigest
                + "'");
      }
    }

    return null;
  }

  @Override
  public URL getApiRoute(String apiRouteBase) throws MalformedURLException {
    return new URL(
        apiRouteBase + registryEndpointProperties.getImageName() + "/blobs/" + blobDigest);
  }

  @Override
  public String getHttpMethod() {
    return HttpMethods.GET;
  }

  public String getActionDescription() {
    return "pull BLOB for "
        + registryEndpointProperties.getServerUrl()
        + "/"
        + registryEndpointProperties.getImageName()
        + " with digest "
        + blobDigest;
  }
}

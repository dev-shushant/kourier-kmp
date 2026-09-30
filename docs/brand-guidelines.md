# AppArmorX Resilience brand guidelines

AppArmorX is the umbrella brand. **AppArmorX Resilience** is the product, described as a **Mobile API Reliability Toolkit**. Use **AppArmorX Resilience, formerly Kourier** on first introduction during the transition.

The product promise is **Reproduce failures. Understand behavior. Ship with confidence.** The roadmap workflow is **observe → experiment → reproduce → explain → share → verify**.

## Current and planned capabilities

Current builds provide on-device HTTP inspection, telemetry, configured redaction, local persistence, and text/HAR/cURL export for supported integrations. Lead current installation and demo copy with those capabilities.

Controlled fault injection, capture-to-mock, portable scenarios, and GraphQL operation matching are planned for the next release. Journey evidence, contract checks, incident capsules, and agent integrations are later milestones. Do not present them as available features.

Resilience provides development and QA reliability tools. AppArmorX security analysis and runtime protection are separate offerings; the inspector does not imply malware protection, production hardening, or security certification.

## Language and visual continuity

- Write **AppArmorX**, with a capital X, and **AppArmorX Resilience** in product copy.
- Use plain feature labels: Inspector, Scenarios, Timeline, Contracts.
- In the compact inspector header, show AppArmorX above Resilience and the request count. Use the complete name in settings, accessibility descriptions, exports, and notifications.
- Retain the existing abstract network mark and UI palette for this transition. The mark contains no Kourier lettering. A visual redesign can follow independently.
- Existing screenshots and videos must be labeled as legacy Kourier builds until replaced with captures of the rebranded build.

## Technical compatibility

Retain `dev.shushant.kourier`, `kourier-*` artifact/module names, `Kourier` facade APIs, `KourierIos`/`KourierSwift` products, framework filenames, repository URLs, storage names, notification identifiers, and sample bundle IDs. Product branding and public symbol naming are separate concerns. See [the transition guide](rebranding.md).

## Public transition copy

> Kourier is becoming AppArmorX Resilience, the mobile API reliability toolkit within AppArmorX. The existing Android and iOS inspector integrations remain compatible. Our next step is controlled API experiments and portable scenarios; those features will arrive incrementally.

Use https://apparmorx.com/ as the umbrella website. `/resilience` is a proposed product page and must not be linked as live until deployed. This repository update does not publish website or social changes.

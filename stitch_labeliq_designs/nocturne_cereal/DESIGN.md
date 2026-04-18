# Design System Strategy: The Digital Sommelier

## 1. Overview & Creative North Star
The North Star for this design system is **"The Digital Sommelier."** Just as a sommelier guides a guest through complex information with understated authority and warmth, this system must transform dense nutritional data into an effortless, editorial experience. 

We are moving away from the "utility app" aesthetic of spreadsheets and toggles. Instead, we embrace **Organic Sophistication**. By utilizing the depth of a dark-mode palette inspired by premium hospitality, we create a high-contrast environment where photography and typography become the primary UI elements. The layout breaks the rigid "boxed" grid through intentional asymmetry—allowing images to bleed to edges and using typography as a structural anchor rather than a container.

## 2. Colors: Tonal Depth & The "No-Line" Rule
This system relies on light and atmosphere rather than lines to define space.

### The "No-Line" Rule
**Explicit Instruction:** You are prohibited from using 1px solid borders for sectioning or grouping. Boundaries must be defined solely through background shifts. Use `surface-container-low` for large section backgrounds and `surface-container-high` for interactive elements. This creates a "soft-touch" interface that feels expensive and seamless.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of materials. 
- **Base Layer:** `background` (#131313)
- **Secondary Sections:** `surface-container-low` (#1C1B1B)
- **Interactive Cards:** `surface-container` (#201F1F)
- **Elevated Modals:** `surface-container-highest` (#353534)

### The "Glass & Gradient" Rule
To avoid a flat, "Material 1.0" look, apply a 20% opacity to `surface-variant` with a 16px backdrop blur for floating headers and navigation bars. Use a subtle linear gradient on primary CTAs—transitioning from `primary` (#FFB2B6) at the top left to `primary-container` (#FF5169) at the bottom right—to give buttons a tactile, "lit from within" glow.

## 3. Typography: The Editorial Voice
We utilize **Plus Jakarta Sans** for its warm, rounded terminals, echoing the approachability of premium travel brands while maintaining technical legibility.

- **Display & Headlines:** Use `display-lg` through `headline-sm` with a **-2% tracking (letter-spacing)**. This creates a "tight," custom-typeset look seen in high-end magazines.
- **Visual Hierarchy:**
    - **Display-LG (3.5rem):** Reserved for hero nutritional scores or impact numbers.
    - **Title-MD (1.125rem):** The workhorse for product names. Use weight 700.
    - **Body-MD (0.875rem):** Use for ingredient lists and descriptions. Weight 400.
    - **Label-SM (0.6875rem):** Use for metadata (e.g., "per 100g"). Weight 700 with +3% tracking for uppercase instances to ensure readability.

## 4. Elevation & Depth
In this system, elevation is an atmospheric effect, not a structural one.

### Tonal Layering
Instead of shadows, stack your containers. Place a `surface-container-lowest` card inside a `surface-container-high` section to create "recessed" depth. This mimics the look of high-end car dashboards or luxury audio equipment.

### Ambient Shadows
When an element must float (like a Floating Action Button or a Tooltip), use a **Three-Layer Dark Shadow**:
1. 0px 4px 12px rgba(0,0,0,0.3)
2. 0px 8px 24px rgba(0,0,0,0.2)
3. 0px 16px 48px rgba(0,0,0,0.1)
This creates a "subtle glow" that feels like an aura rather than a harsh drop shadow.

### The "Ghost Border" Fallback
If contrast is insufficient (e.g., in accessibility audits), use a "Ghost Border." Apply `outline-variant` (#5C3F41) at **15% opacity**. This provides a hint of a container without breaking the "No-Line" rule.

## 5. Components

### Buttons
- **Primary:** `primary` background with `on-primary` text. 8px radius (`md`).
- **Secondary:** Glassmorphism style. `surface-bright` at 10% opacity, backdrop blur 12px, Ghost Border (15% `outline`).
- **Interaction:** On press, scale the button down to 96%—never change the color opacity.

### Nutrition Cards
- **Construction:** 20px radius (`xl`). No border. 
- **Photography:** Use a "Photography-Forward" approach. Hero cards should feature a blurred, high-contrast crop of the food item as a background, overlaid with a `surface-dim` gradient (70% opacity to 100% opacity) to ensure text legibility.
- **The Divider Rule:** Forbid 1px dividers. Use a 16px vertical gap (`lg` spacing) or a slight background shift (`surface-container-low` to `surface-container-high`) to separate content blocks.

### Scanner Interface (Signature Component)
- **The Reticle:** Instead of a square box, use four corner "L" shapes using the `secondary` (#5BDACC) color. 
- **The Glow:** Apply a 20px blur to the `primary` accent behind the "Scan" button to make the brand moment feel like it's emitting light.

### Chips & Tags
- **Success/Safe:** `secondary-container` background with `on-secondary-container` text.
- **Danger/Avoid:** `primary-container` background with `on-primary-container` text.
- Shape: Always `full` radius (pill shape) for tags.

## 6. Do's and Don'ts

### Do
- **Do** allow images to "breathe" by using generous whitespace (24px - 32px) between sections.
- **Do** use negative tracking on large headlines to create a sophisticated, editorial feel.
- **Do** use `secondary` (vibrant green) for positive health attributes to trigger an immediate "safe" psychological response.

### Don't
- **Don't** use 100% white (#FFFFFF). Only use `on-background` (#E5E2E1) to reduce eye strain in dark mode.
- **Don't** use 1px dividers. If you feel you need one, use a 4px height bar with a 5% opacity or simply more whitespace.
- **Don't** use standard "drop shadows." If it doesn't look like a soft ambient glow, it's too heavy.
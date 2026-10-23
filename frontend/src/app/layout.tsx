import type { ReactNode } from "react";
import { AssistantPanel } from "../components/AssistantPanel";

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <main>{children}</main>
        <AssistantPanel />
      </body>
    </html>
  );
}

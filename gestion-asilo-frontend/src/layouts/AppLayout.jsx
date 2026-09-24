import { Navbar } from "../components/layout/Navbar";
import { Sidebar } from "../components/layout/Sidebar";

export function AppLayout({ migas, children }) {
  return (
    <div className="flex min-h-screen bg-fondo">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar migas={migas} />
        <main className="flex-1 px-6 py-8">{children}</main>
      </div>
    </div>
  );
}

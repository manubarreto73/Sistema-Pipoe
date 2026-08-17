import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Outlet } from "react-router";

import { queryClient } from "@/lib/queryClient";
import { AuthBootstrap } from "@/router/AuthBootstrap";

export function Providers() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrap>
        <Outlet />
      </AuthBootstrap>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}

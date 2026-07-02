"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { UserPlus } from "lucide-react";
import { adminApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, humanize } from "@/lib/format";
import type { Role, UserResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Select, Textarea } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, TBody, Td, Th, THead, Tr } from "@/components/ui/table";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

const staffSchema = z.object({
  fullName: z.string().min(2, "Enter the full name"),
  email: z.string().email("Enter a valid email"),
  phone: z.string().optional().or(z.literal("")),
  password: z.string().min(8, "At least 8 characters"),
  role: z.enum(["LOAN_OFFICER", "ADMIN"]),
});
type StaffForm = z.infer<typeof staffSchema>;

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [roleFilter, setRoleFilter] = useState<Role | "">("");
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<UserResponse | null>(null);
  const [reason, setReason] = useState("");

  const users = useQuery({
    queryKey: ["admin", "users", roleFilter, page],
    queryFn: () => adminApi.users({ role: roleFilter || undefined, page, size: 20 }),
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] });

  const form = useForm<StaffForm>({
    resolver: zodResolver(staffSchema),
    defaultValues: { role: "LOAN_OFFICER" },
  });

  const create = useMutation({
    mutationFn: (values: StaffForm) =>
      adminApi.createUser({ ...values, phone: values.phone || undefined }),
    onSuccess: () => {
      toast.success("Account created");
      setCreateOpen(false);
      form.reset();
      refresh();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const deactivate = useMutation({
    mutationFn: () => adminApi.deactivateUser(deactivateTarget!.id, reason),
    onSuccess: () => {
      toast.success("Account deactivated");
      setDeactivateTarget(null);
      setReason("");
      refresh();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const activate = useMutation({
    mutationFn: (userId: string) => adminApi.activateUser(userId),
    onSuccess: () => { toast.success("Account reactivated"); refresh(); },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">User management</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <UserPlus className="h-4 w-4" aria-hidden /> Create staff account
        </Button>
      </div>

      <Card>
        <CardHeader className="flex-row items-end justify-between">
          <div>
            <CardTitle>Accounts</CardTitle>
            <CardDescription>
              Borrowers self-register; staff are provisioned here. Deactivation keeps history but
              blocks sign-in.
            </CardDescription>
          </div>
          <div className="w-44">
            <Label>Role</Label>
            <Select
              value={roleFilter}
              onChange={(e) => { setRoleFilter(e.target.value as Role | ""); setPage(0); }}
            >
              <option value="">All roles</option>
              <option value="BORROWER">Borrowers</option>
              <option value="LOAN_OFFICER">Loan officers</option>
              <option value="ADMIN">Admins</option>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {users.isLoading && <LoadingState label="Loading users…" />}
          {users.isError && (
            <ErrorState message={errorMessage(users.error)} onRetry={() => users.refetch()} />
          )}
          {users.data && users.data.items.length === 0 && (
            <EmptyState title="No users match this filter" />
          )}
          {users.data && users.data.items.length > 0 && (
            <>
              <Table>
                <THead>
                  <Tr>
                    <Th>Name</Th>
                    <Th>Email</Th>
                    <Th>Role</Th>
                    <Th>Status</Th>
                    <Th>Created</Th>
                    <Th />
                  </Tr>
                </THead>
                <TBody>
                  {users.data.items.map((user) => (
                    <Tr key={user.id}>
                      <Td className="font-medium text-slate-900">{user.fullName}</Td>
                      <Td>{user.email}</Td>
                      <Td>{humanize(user.role)}</Td>
                      <Td>
                        <Badge tone={user.active ? "green" : "red"}>
                          {user.active ? "Active" : "Deactivated"}
                        </Badge>
                      </Td>
                      <Td>{formatDate(user.createdAt)}</Td>
                      <Td className="text-right">
                        {user.active ? (
                          <Button variant="ghost" size="sm" onClick={() => setDeactivateTarget(user)}>
                            Deactivate
                          </Button>
                        ) : (
                          <Button variant="ghost" size="sm" onClick={() => activate.mutate(user.id)}>
                            Reactivate
                          </Button>
                        )}
                      </Td>
                    </Tr>
                  ))}
                </TBody>
              </Table>
              <div className="mt-3 flex items-center justify-between text-sm text-slate-500">
                <span>
                  Page {users.data.page + 1} of {Math.max(users.data.totalPages, 1)} ·{" "}
                  {users.data.totalItems} account(s)
                </span>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= users.data.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Create a staff account"
        description="The new staff member should change their password after first sign-in."
      >
        <form onSubmit={form.handleSubmit((v) => create.mutate(v))} className="space-y-3" noValidate>
          <div>
            <Label>Full name</Label>
            <Input {...form.register("fullName")} />
            <FieldError message={form.formState.errors.fullName?.message} />
          </div>
          <div>
            <Label>Email</Label>
            <Input type="email" {...form.register("email")} />
            <FieldError message={form.formState.errors.email?.message} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Role</Label>
              <Select {...form.register("role")}>
                <option value="LOAN_OFFICER">Loan officer</option>
                <option value="ADMIN">Admin</option>
              </Select>
            </div>
            <div>
              <Label>Phone</Label>
              <Input {...form.register("phone")} />
            </div>
          </div>
          <div>
            <Label>Temporary password</Label>
            <Input type="password" {...form.register("password")} />
            <FieldError message={form.formState.errors.password?.message} />
          </div>
          <Button type="submit" className="w-full" loading={create.isPending}>
            Create account
          </Button>
        </form>
      </Dialog>

      <Dialog
        open={!!deactivateTarget}
        onClose={() => setDeactivateTarget(null)}
        title={`Deactivate ${deactivateTarget?.fullName ?? ""}?`}
        description="They will be blocked from signing in. Their history is preserved. This is audit-logged."
      >
        <div className="space-y-3">
          <div>
            <Label>Reason (required, min 10 characters)</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <Button
            variant="destructive"
            className="w-full"
            disabled={reason.length < 10}
            loading={deactivate.isPending}
            onClick={() => deactivate.mutate()}
          >
            Confirm deactivation
          </Button>
        </div>
      </Dialog>
    </div>
  );
}

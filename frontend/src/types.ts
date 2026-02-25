export interface IdolDto {
  idolId: number;
  name: string;
  groupName: string;
  imageUrl: string;
  groupId: number;
}

export interface GroupDto {
  groupId: number;
  name: string;
  description: string;
  imageUrl: string;
}

export interface SubscriptionDto {
  subscriptionId: number;
  userId: number;
  idolId: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}
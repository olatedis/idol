import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import type {IdolDto, GroupDto, SubscriptionDto} from '../../types';

interface IdolWithCount extends IdolDto {
  subscriberCount: number;
}

interface GroupWithIdols extends GroupDto {
  idols: IdolWithCount[];
}

const IdolPage: React.FC = () => {
  const navigate = useNavigate();
  const [subscribedIdols, setSubscribedIdols] = useState<IdolDto[]>([]);
  const [groups, setGroups] = useState<GroupWithIdols[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      await Promise.all([fetchSubscriptions(), fetchGroupsAndIdols()]);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchSubscriptions = async () => {
    try {
      const { data: subs } = await api.get<SubscriptionDto[]>('/subscriptions/me');
      const idolPromises = subs.map(sub =>
        api.get(`/idols/${sub.idolId}`).then(res => res.data)
      );
      const idols = await Promise.all(idolPromises);
      setSubscribedIdols(idols);
    } catch (error) {
      console.error('Failed to fetch subscriptions:', error);
    }
  };

  const fetchGroupsAndIdols = async () => {
    try {
      const { data: groups } = await api.get<GroupDto[]>('/groups');
      const groupWithIdols = await Promise.all(groups.map(async group => {
        const { data: idols } = await api.get<IdolDto[]>(`/groups/${group.groupId}/idols`);
        const idolsWithCount = await Promise.all(idols.map(async idol => {
          const { data: count } = await api.get<number>(`/subscriptions/count/${idol.idolId}`);
          return { ...idol, subscriberCount: count };
        }));
        idolsWithCount.sort((a, b) => b.subscriberCount - a.subscriberCount);
        return { ...group, idols: idolsWithCount };
      }));
      setGroups(groupWithIdols);
    } catch (error) {
      console.error('Failed to fetch groups and idols:', error);
    }
  };

  const handleClick = (idol: IdolDto) => {
    navigate(`/group/${idol.groupId}`);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-6">아이돌 목록</h1>

      {/* 구독중인 아이돌 */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-4">구독중인 아이돌</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {subscribedIdols.map(idol => (
            <div key={idol.idolId} className="bg-white p-4 rounded shadow cursor-pointer" onClick={() => handleClick(idol)}>
              <img src={idol.imageUrl} alt={idol.name} className="w-full h-48 object-cover rounded mb-2" />
              <h3 className="text-lg font-medium">{idol.name}</h3>
              <p className="text-gray-600">{idol.groupName}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 전체 아이돌 */}
      <section>
        <h2 className="text-xl font-semibold mb-4">전체 아이돌</h2>
        {groups.map(group => (
          <div key={group.groupId} className="mb-6">
            <h3 className="text-lg font-medium mb-2">{group.name}</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {group.idols.map(idol => (
                <div key={idol.idolId} className="bg-white p-4 rounded shadow cursor-pointer" onClick={() => handleClick(idol)}>
                  <img src={idol.imageUrl} alt={idol.name} className="w-full h-48 object-cover rounded mb-2" />
                  <h4 className="text-lg font-medium">{idol.name}</h4>
                  <p className="text-gray-600">{group.name}</p>
                  <p className="text-sm text-gray-500">구독자: {idol.subscriberCount}</p>
                </div>
              ))}
            </div>
          </div>
        ))}
      </section>
    </div>
  );
};

export default IdolPage;